package com.tutoroo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.AssessmentDTO;
import com.tutoroo.dto.StudyDTO;
import com.tutoroo.entity.MembershipTier;
import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyService {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiChatModel chatModel;

    // =================================================================================
    // 1. 학습 플랜 생성 및 관리 (CRUD)
    // =================================================================================

    @Transactional
    public Long createPlan(Long userId, StudyDTO.CreatePlanRequest request) {
        // [검증] 플랜 생성 가능 여부 확인
        validatePlanCreationLimit(userId);

        String initialRoadmap = "{}"; // 초기 로드맵은 빈 값 (AssessmentService에서 생성)

        StudyPlanEntity plan = StudyPlanEntity.builder()
                .userId(userId)
                .goal(request.goal())
                .persona(request.teacherType())
                .customTutorName(resolveTutorName(request.teacherType()))
                .roadmapJson(initialRoadmap)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .progressRate(0.0)
                .currentLevel("BEGINNER")
                .targetLevel("INTERMEDIATE")
                .isPaid(false) // 기본값 false, 결제 연동 시 수정
                .status("PROCEEDING")
                .build();

        studyMapper.savePlan(plan);
        return plan.getId();
    }

    @Transactional
    public void deleteStudyPlan(Long userId, Long planId) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);
        if (!plan.getUserId().equals(userId)) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        studyMapper.deletePlan(planId);
        // 관련 세션 데이터도 정리
        clearSessionState(planId);
        log.info("🗑️ 학습 플랜 및 세션 삭제 완료: userId={}, planId={}", userId, planId);
    }

    // =================================================================================
    // 2. 조회 로직 (상세, 상태, 목록, 캘린더)
    // =================================================================================

    @Transactional(readOnly = true)
    public StudyDTO.StudyStatusResponse getStudyStatus(Long userId, Long planId) {
        StudyPlanEntity plan;
        if (planId == null) {
            List<StudyPlanEntity> activePlans = studyMapper.findActivePlansByUserId(userId);
            if (activePlans.isEmpty()) return null;
            plan = activePlans.get(0);
        } else {
            plan = studyMapper.findById(planId);
        }

        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);
        }

        StudyLogEntity lastLog = studyMapper.findLatestLogByPlanId(plan.getId());
        int currentDay = (lastLog == null) ? 1 : lastLog.getDayCount() + 1;
        String lastTopic = (lastLog == null) ? "오리엔테이션" : lastLog.getContentSummary();

        // 오늘 학습 완료 여부 체크
        boolean isResting = false;
        if (lastLog != null && lastLog.getStudyDate().toLocalDate().isEqual(LocalDate.now()) && Boolean.TRUE.equals(lastLog.getIsCompleted())) {
            isResting = true;
            currentDay = lastLog.getDayCount(); // 이미 완료했으면 day 유지
        }

        return StudyDTO.StudyStatusResponse.builder()
                .planId(plan.getId())
                .goal(plan.getGoal())
                .personaName(plan.getCustomTutorName())
                .currentDay(currentDay)
                .progressRate(plan.getProgressRate())
                .isResting(isResting)
                .lastTopic(lastTopic)
                .build();
    }

    @Transactional(readOnly = true)
    public StudyDTO.PlanDetailResponse getPlanDetail(Long userId, Long planId) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);
        }
        return buildPlanDetailResponse(plan);
    }

    @Transactional(readOnly = true)
    public StudyDTO.PlanDetailResponse getCurrentPlanDetail(Long userId) {
        List<StudyPlanEntity> activePlans = studyMapper.findActivePlansByUserId(userId);
        if (activePlans.isEmpty()) return null;
        return buildPlanDetailResponse(activePlans.get(0));
    }

    @Transactional(readOnly = true)
    public List<StudyDTO.StudySimpleInfo> getActiveStudyList(Long userId) {
        return studyMapper.findActivePlansByUserId(userId).stream()
                .map(plan -> new StudyDTO.StudySimpleInfo(
                        plan.getId(),
                        plan.getGoal(),
                        plan.getCustomTutorName() != null ? plan.getCustomTutorName() : plan.getPersona()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudyDTO.CalendarResponse getMonthlyCalendar(Long userId, int year, int month) {
        List<StudyLogEntity> logs = studyMapper.findLogsByUserIdAndMonth(userId, year, month);

        List<StudyDTO.DailyLog> dailyLogs = logs.stream()
                .map(l -> StudyDTO.DailyLog.builder()
                        .day(l.getStudyDate().getDayOfMonth())
                        .isDone(Boolean.TRUE.equals(l.getIsCompleted()))
                        .score(l.getTestScore() != null ? l.getTestScore() : 0)
                        .topic(l.getContentSummary())
                        .build())
                .collect(Collectors.toList());

        // 중복 날짜 제거 및 병합 로직이 필요하다면 여기서 처리 (현재는 단순 리스트 반환)
        return StudyDTO.CalendarResponse.builder()
                .year(year)
                .month(month)
                .totalStudyDays(logs.size())
                .logs(dailyLogs)
                .build();
    }

    // =================================================================================
    // 3. 학습 로그 및 진도율 관리
    // =================================================================================

    @Transactional
    public void saveSimpleLog(Long userId, StudyDTO.StudyLogRequest request) {
        StudyPlanEntity plan = studyMapper.findById(request.planId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);
        }

        // [동시성 제어] 따닥 방지
        List<StudyLogEntity> todayLogs = studyMapper.findLogsByUserIdAndDate(userId, LocalDate.now());
        boolean alreadyStudiedToday = todayLogs.stream()
                .anyMatch(log -> log.getPlanId().equals(plan.getId()));

        if (alreadyStudiedToday) {
            log.warn("⛔ 중복 학습 로그 저장 차단: PlanID {}", plan.getId());
            return;
        }

        StudyLogEntity lastLog = studyMapper.findLatestLogByPlanId(plan.getId());
        int newDayCount = (lastLog == null) ? 1 : lastLog.getDayCount() + 1;

        StudyLogEntity logEntity = StudyLogEntity.builder()
                .planId(plan.getId())
                .dayCount(newDayCount)
                .studyDate(LocalDateTime.now())
                .testScore(request.score())
                .contentSummary(request.contentSummary())
                .dailySummary("오늘의 학습: " + request.contentSummary())
                .isCompleted(request.isCompleted())
                .pointChange(request.score() > 0 ? request.score() : 10)
                .build();

        studyMapper.saveLog(logEntity);
        userMapper.earnPoints(userId, logEntity.getPointChange());
        updateProgress(plan.getId(), calculateProgress(plan, newDayCount));

        log.info("📝 학습 로그 저장 완료: User={}, Plan={}, Day={}", userId, plan.getId(), newDayCount);
    }

    @Transactional
    public void updateProgress(Long planId, int progressPercent) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) return;
        plan.setProgressRate((double) progressPercent);
        studyMapper.updateProgress(plan);
    }

    @Transactional(readOnly = true)
    public boolean canCreateNewGoal(Long userId) {
        try {
            validatePlanCreationLimit(userId);
            return true;
        } catch (TutorooException e) {
            return false;
        }
    }

    // =================================================================================
    // 4. AI 채팅 및 피드백 (핵심 로직 - Transactional 분리)
    // =================================================================================

    /**
     * [AI 채팅 핸들러]
     * - 선제적 개입, 로드맵 바인딩, 시각화 유도 적용
     * - DB 트랜잭션 없이 실행하여 성능 최적화
     */
    public StudyDTO.ChatResponse handleSimpleChat(Long userId, Long planId, String userMessage) {
        UserEntity user = userMapper.findById(userId);
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        StudyLogEntity lastLog = studyMapper.findLatestLogByPlanId(planId);
        String historyKey = "chat:history:" + planId;
        List<String> history = redisTemplate.opsForList().range(historyKey, 0, 9);

        // [핵심] 지능형 페르소나 생성
        String systemPrompt = buildSmartSystemPersona(user, plan, lastLog, history);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        if (history != null) {
            for (String h : history) {
                if (h.startsWith("user:")) messages.add(new UserMessage(h.substring(5)));
                else if (h.startsWith("assistant:")) messages.add(new AssistantMessage(h.substring(10)));
            }
        }
        messages.add(new UserMessage(userMessage));

        Prompt prompt = new Prompt(messages);

        // AI 호출 (Spring AI 1.0.0-M6 호환)
        String aiResponseText = chatModel.call(prompt).getResult().getOutput().getText();
        String cleanedResponse = cleanText(aiResponseText);

        // Redis에 대화 내역 저장
        redisTemplate.opsForList().rightPush(historyKey, "user:" + userMessage);
        redisTemplate.opsForList().rightPush(historyKey, "assistant:" + cleanedResponse);
        redisTemplate.expire(historyKey, 1, TimeUnit.HOURS);

        return StudyDTO.ChatResponse.builder()
                .aiMessage(cleanedResponse)
                .audioUrl(null)
                .build();
    }

    /**
     * [AI 피드백 생성]
     * - 학습 로그 기반 상세 피드백 생성
     */
    public String generateAiFeedbackByPlanId(Long planId) {
        StudyLogEntity logEntity = studyMapper.findLatestLogByPlanId(planId);
        if (logEntity == null) return "아직 학습 기록이 부족하여 피드백을 생성할 수 없습니다.";

        StudyPlanEntity plan = studyMapper.findById(planId);
        UserEntity user = userMapper.findById(plan.getUserId());

        String promptText = String.format("""
            [역할: %s]
            학생: %s (%d세)
            목표: %s
            오늘 학습: %s (점수: %d)
            소감: %s
            
            위 내용을 바탕으로 200자 이내의 따뜻하고 구체적인 피드백을 작성해.
            말투는 반드시 역할에 맞춰서 해.
            """,
                resolveTutorName(plan.getPersona()),
                user.getName(), user.getAge(), plan.getGoal(),
                logEntity.getContentSummary(), logEntity.getTestScore(),
                logEntity.getStudentFeedback() != null ? logEntity.getStudentFeedback() : "없음"
        );

        String response = chatModel.call(promptText);
        String cleaned = cleanText(response);

        // 저장 로직 (Mapper는 Auto-commit 되므로 별도 트랜잭션 불필요)
        studyMapper.updateAiFeedbackSuccess(logEntity.getId(), cleaned);
        return cleaned;
    }

    // =================================================================================
    // 5. 유틸리티 및 헬퍼 메서드
    // =================================================================================

    private void validatePlanCreationLimit(Long userId) {
        UserEntity user = userMapper.findById(userId);
        int currentCount = studyMapper.countActivePlansByUserId(userId);
        MembershipTier tier = user.getEffectiveTier();

        if (currentCount >= tier.getMaxActiveGoals()) {
            throw new TutorooException(
                    String.format("등급(%s) 제한: 최대 %d개의 목표만 생성 가능합니다.", tier.name(), tier.getMaxActiveGoals()),
                    ErrorCode.MULTIPLE_PLANS_REQUIRED_PAYMENT
            );
        }
    }

    // [New] 스마트 페르소나 빌더
    private String buildSmartSystemPersona(UserEntity user, StudyPlanEntity plan, StudyLogEntity lastLog, List<String> history) {
        String currentTopic = extractCurrentTopic(plan.getRoadmapJson(), lastLog);
        StringBuilder sb = new StringBuilder();

        sb.append(getPersonaDescription(plan.getPersona())).append("\n\n");
        sb.append("학생: ").append(user.getName()).append(" (").append(user.getAge()).append("세)\n");
        sb.append("현재 주제: ").append(currentTopic).append("\n");

        sb.append("[규칙]\n");
        sb.append("1. 주제('").append(currentTopic).append("')를 벗어나는 잡담은 정중히 차단하고 수업으로 복귀.\n");
        sb.append("2. 정답 대신 질문을 던져 스스로 깨닫게 유도 (소크라테스법).\n");
        sb.append("3. 구조적 설명이 필요하면 'Mermaid.js' 코드를 생성.\n");
        sb.append("4. 코드는 반드시 마크다운(```java) 사용.\n");

        // 선제적 개입 (히스토리 없을 때)
        if (history == null || history.isEmpty()) {
            sb.append("\n[지시] 대화 시작 시, 밝게 인사하며 '").append(currentTopic).append("' 학습을 시작하자고 먼저 제안해.");
        }
        return sb.toString();
    }

    private String extractCurrentTopic(String roadmapJson, StudyLogEntity lastLog) {
        try {
            if (!StringUtils.hasText(roadmapJson)) return "기초 학습";
            JsonNode root = objectMapper.readTree(roadmapJson);
            int currentDay = (lastLog == null) ? 1 : lastLog.getDayCount() + 1;
            if (root.has("chapters")) {
                for (JsonNode chapter : root.get("chapters")) {
                    if (chapter.has("dailyTasks")) {
                        // 실제로는 dayCount 매핑 로직이 더 복잡할 수 있음
                        return chapter.get("title").asText() + " (Day " + currentDay + ")";
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return "자율 학습";
    }

    private StudyDTO.PlanDetailResponse buildPlanDetailResponse(StudyPlanEntity plan) {
        AssessmentDTO.RoadmapData roadmapData = null;
        try {
            if (StringUtils.hasText(plan.getRoadmapJson())) {
                roadmapData = objectMapper.readValue(plan.getRoadmapJson(), AssessmentDTO.RoadmapData.class);
            }
        } catch (Exception e) {
            log.error("로드맵 파싱 오류", e);
        }
        return StudyDTO.PlanDetailResponse.builder()
                .planId(plan.getId())
                .goal(plan.getGoal())
                .persona(plan.getPersona())
                .customTutorName(plan.getCustomTutorName())
                .progressRate(plan.getProgressRate())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .roadmap(roadmapData)
                .daysRemaining(plan.getDaysRemaining())
                .build();
    }

    private String getPersonaDescription(String type) {
        return switch (type) {
            case "TIGER" -> "너는 엄격한 호랑이 선생님. 반말 사용. '정신 차려!'가 입버릇.";
            case "RABBIT" -> "너는 성격 급한 토끼 선생님. 핵심만 빠르게 설명.";
            case "TURTLE" -> "너는 친절한 거북이 선생님. 존댓말 사용. 기초부터 차근차근.";
            case "KANGAROO" -> "너는 열정적인 캥거루 선생님. '할 수 있어!'라고 계속 격려.";
            case "EASTERN_DRAGON" -> "너는 지혜로운 청룡 선생님. 하오체 사용.";
            default -> "너는 친절하고 전문적인 AI 선생님.";
        };
    }

    private String cleanText(String text) {
        if (text == null) return "";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String resolveTutorName(String type) {
        if (type == null) return "AI 튜터";
        return switch (type) {
            case "TIGER" -> "호랑이 선생님";
            case "RABBIT" -> "토끼 선생님";
            case "TURTLE" -> "거북이 선생님";
            case "KANGAROO" -> "캥거루 선생님";
            case "EASTERN_DRAGON" -> "청룡 선생님";
            default -> "AI 튜터";
        };
    }

    private int calculateProgress(StudyPlanEntity plan, int currentDay) {
        if (plan.getEndDate() == null) return Math.min(100, (int) ((double) currentDay / 30.0 * 100));
        long totalDays = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate());
        return Math.min(100, Math.max(0, (int) ((double) currentDay / (totalDays <= 0 ? 1 : totalDays) * 100)));
    }

    // [복구] Redis 세션 관리 유틸리티
    public void saveSessionState(Long planId, String stateJson) {
        redisTemplate.opsForValue().set("session:" + planId, stateJson, 24, TimeUnit.HOURS);
    }

    public String getSessionState(Long planId) {
        return redisTemplate.opsForValue().get("session:" + planId);
    }

    public void clearSessionState(Long planId) {
        redisTemplate.delete("session:" + planId);
    }
}