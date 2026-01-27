package com.tutoroo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.AssessmentDTO;
import com.tutoroo.dto.StudyDTO;
import com.tutoroo.dto.TutorDTO;
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
    private final TutorService tutorService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiChatModel chatModel;

    // --- [1] 현재 학습 플랜 상세 조회 (Step 5: 대시보드/로드맵) ---
    @Transactional(readOnly = true)
    public StudyDTO.PlanDetailResponse getCurrentPlanDetail(Long userId) {
        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(userId);
        if (plans.isEmpty()) {
            return null;
        }
        StudyPlanEntity currentPlan = plans.get(0);

        AssessmentDTO.RoadmapData roadmapData = null;
        try {
            if (StringUtils.hasText(currentPlan.getRoadmapJson())) {
                roadmapData = objectMapper.readValue(currentPlan.getRoadmapJson(), AssessmentDTO.RoadmapData.class);
            }
        } catch (JsonProcessingException e) {
            log.error("⚠️ 로드맵 JSON 파싱 실패 (PlanId: {}): {}", currentPlan.getId(), e.getMessage());
        }

        return StudyDTO.PlanDetailResponse.builder()
                .planId(currentPlan.getId())
                .goal(currentPlan.getGoal())
                .persona(currentPlan.getPersona())
                .customTutorName(currentPlan.getCustomTutorName())
                .progressRate(currentPlan.getProgressRate())
                .startDate(currentPlan.getStartDate())
                .endDate(currentPlan.getEndDate())
                .roadmap(roadmapData)
                .daysRemaining(currentPlan.getDaysRemaining())
                .build();
    }

    @Transactional(readOnly = true)
    public StudyDTO.PlanDetailResponse getPlanDetail(Long userId, Long planId) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        if (!plan.getUserId().equals(userId)) {
            throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        AssessmentDTO.RoadmapData roadmapData = null;
        try {
            if (StringUtils.hasText(plan.getRoadmapJson())) {
                roadmapData = objectMapper.readValue(plan.getRoadmapJson(), AssessmentDTO.RoadmapData.class);
            }
        } catch (JsonProcessingException e) {
            log.error("⚠️ 로드맵 JSON 파싱 실패 (PlanId: {}): {}", plan.getId(), e.getMessage());
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

    // --- [2] 현재 학습 상태 요약 (메인 홈 위젯용) ---
    @Transactional(readOnly = true)
    public StudyDTO.StudyStatusResponse getCurrentStudyStatus(Long userId, Long planId) {
        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(userId);
        if (plans.isEmpty()) return null;

        StudyPlanEntity currentPlan = plans.stream()
                .filter(p -> planId == null || p.getId().equals(planId))
                .findFirst()
                .orElse(plans.get(0));

        List<StudyLogEntity> todayLogs = studyMapper.findLogsByUserIdAndDate(userId, LocalDate.now());
        boolean isResting = !todayLogs.isEmpty() && todayLogs.stream()
                .filter(log -> log.getPlanId().equals(currentPlan.getId()))
                .anyMatch(StudyLogEntity::getIsCompleted);

        String lastTopic = todayLogs.isEmpty() ? "새로운 학습을 시작해보세요!" : todayLogs.get(0).getContentSummary();

        return StudyDTO.StudyStatusResponse.builder()
                .planId(currentPlan.getId())
                .goal(currentPlan.getGoal())
                .personaName(currentPlan.getPersona())
                .currentDay(studyMapper.findLogsByPlanId(currentPlan.getId()).size() + 1)
                .progressRate(currentPlan.getProgressRate())
                .isResting(isResting)
                .lastTopic(lastTopic)
                .build();
    }

    // --- [3] 학습 로그 저장 및 진도율 업데이트 ---
    @Transactional
    public void saveSimpleLog(Long userId, StudyDTO.StudyLogRequest request) {
        StudyPlanEntity plan = studyMapper.findById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        StudyLogEntity logEntity = StudyLogEntity.builder()
                .planId(plan.getId())
                .studyDate(LocalDateTime.now())
                .dayCount(request.dayCount())
                .contentSummary(request.contentSummary())
                .testScore(request.score())
                .isCompleted(request.isCompleted())
                .pointChange(request.score() > 0 ? request.score() : 10)
                .build();
        studyMapper.saveLog(logEntity);

        userMapper.earnPoints(userId, logEntity.getPointChange());

        int newProgress = calculateProgress(plan, request.dayCount());
        updateProgress(plan.getId(), newProgress);

        log.info("📝 학습 로그 저장 완료: User={}, Plan={}, Day={}", userId, plan.getId(), request.dayCount());
    }

    // --- [4] 채팅 핸들링 (커리큘럼 조정 등) ---
    @Transactional
    public StudyDTO.ChatResponse handleSimpleChat(Long userId, String message) {
        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(userId);
        if (plans.isEmpty()) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        // [에러 수정 부분]
        // TutorService.adjustCurriculum 메서드가 'needsTts' 파라미터를 요구하도록 변경되었으므로
        // 여기서도 값을 넘겨줘야 합니다.
        // * Simple Chat은 현재 TTS On/Off 플래그를 받지 않으므로 기본값 true(생성함)를 전달합니다.
        TutorDTO.FeedbackChatResponse tutorResponse = tutorService.adjustCurriculum(
                userId,
                plans.get(0).getId(),
                message,
                true // [Fix] needsTts 기본값 (true) 전달
        );

        return StudyDTO.ChatResponse.builder()
                .aiMessage(tutorResponse.aiResponse())
                .audioUrl(tutorResponse.audioUrl())
                .build();
    }

    // --- [5] 활성 학습 목록 조회 (사이드바/메뉴용) ---
    @Transactional(readOnly = true)
    public List<StudyDTO.StudySimpleInfo> getActiveStudyList(Long userId) {
        return studyMapper.findActivePlansByUserId(userId).stream()
                .map(plan -> StudyDTO.StudySimpleInfo.builder()
                        .id(plan.getId())
                        .name(plan.getGoal())
                        .tutor(StringUtils.hasText(plan.getCustomTutorName()) ? plan.getCustomTutorName() : plan.getPersona())
                        .build())
                .collect(Collectors.toList());
    }

    // --- [6] 학습 플랜 삭제 (유저 요청) ---
    @Transactional
    public void deleteStudyPlan(Long userId, Long planId) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) {
            throw new TutorooException("존재하지 않는 학습 플랜입니다.", ErrorCode.STUDY_PLAN_NOT_FOUND);
        }

        if (!plan.getUserId().equals(userId)) {
            throw new TutorooException("본인의 학습 플랜만 삭제할 수 있습니다.", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        studyMapper.deletePlan(planId);
        log.info("🗑️ 학습 플랜 삭제 완료: userId={}, planId={}", userId, planId);
    }

    // --- [7] 캘린더 데이터 (Step 5 상세) ---
    @Transactional(readOnly = true)
    public StudyDTO.CalendarResponse getMonthlyCalendar(Long userId, int year, int month) {
        List<StudyLogEntity> logs = studyMapper.findLogsByUserIdAndMonth(userId, year, month);

        var logsByDay = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getStudyDate().getDayOfMonth()));

        List<StudyDTO.DailyLog> dailyLogs = new ArrayList<>();
        int totalStudyDays = 0;

        for (var entry : logsByDay.entrySet()) {
            int day = entry.getKey();
            List<StudyLogEntity> dayLogs = entry.getValue();

            boolean isDone = dayLogs.stream().anyMatch(StudyLogEntity::getIsCompleted);
            if (isDone) totalStudyDays++;

            int maxScore = dayLogs.stream()
                    .mapToInt(l -> l.getTestScore() != null ? l.getTestScore() : 0)
                    .max().orElse(0);
            String topic = dayLogs.isEmpty() ? "" : dayLogs.get(0).getContentSummary();

            dailyLogs.add(new StudyDTO.DailyLog(day, isDone, maxScore, topic));
        }

        return StudyDTO.CalendarResponse.builder()
                .year(year).month(month)
                .totalStudyDays(totalStudyDays)
                .logs(dailyLogs)
                .build();
    }

    // --- [Helper] Step 18: 멤버십 기반 플랜 생성 제한 확인 ---
    @Transactional(readOnly = true)
    public boolean canCreateNewGoal(Long userId) {
        try {
            validatePlanCreationLimit(userId);
            return true;
        } catch (TutorooException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public void validatePlanCreationLimit(Long userId) {
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

    @Transactional
    public void updateProgress(Long planId, Integer rate) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan != null) {
            plan.setProgressRate((double) rate);
            studyMapper.updateProgress(plan);
        }
    }

    // [New] 스마트 진도율 계산 로직
    private int calculateProgress(StudyPlanEntity plan, int currentDay) {
        if (plan.getEndDate() == null || plan.getStartDate() == null) {
            return Math.min(100, (int) ((double) currentDay / 30.0 * 100));
        }

        long totalDays = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate());
        if (totalDays <= 0) totalDays = 1;

        int percent = (int) ((double) currentDay / totalDays * 100);
        return Math.min(100, Math.max(0, percent));
    }

    @Transactional
    public String generateAiFeedbackByPlanId(Long planId) {
        StudyLogEntity log = studyMapper.findLatestLogByPlanId(planId);
        if (log == null) {
            throw new IllegalArgumentException("해당 플랜에 학습 로그가 없습니다. planId=" + planId);
        }

        Long logId = log.getId();
        studyMapper.updateAiFeedbackPending(logId);

        try {
            String feedback = openAiMakeFeedback(log);
            studyMapper.updateAiFeedbackSuccess(logId, feedback);
            return feedback; //
        } catch (Exception e) {
            studyMapper.updateAiFeedbackFailed(logId);
            throw e;
        }
    }
    private String openAiMakeFeedback(StudyLogEntity log) {
        String prompt = String.format("""
        너는 Tutoroo의 '학습 리포트 작성' 전문 코치다.
        아래 학습 로그만 근거로 한국어 피드백을 작성한다. (추측 금지)

        [출력 형식 - 반드시 그대로]
        1) 한 줄 요약: (학습 상태를 점수/내용 기반으로 요약, 1문장)
        2) ✔️ 잘한 점 1: (contentSummary/dailySummary에서 근거 포함, 1문장)
        3) ✔️ 잘한 점 2: (근거 포함, 1문장)
        4) ⚠️ 개선 포인트 1: (구체적 실수/약점 가정 금지, 대신 "다음에 점검할 항목"으로 제안, 1문장)
        5) ⚠️ 개선 포인트 2: (구체적 점검 항목, 1문장)
        6) 🎯 다음 액션 1: (오늘 내용 기준 10~15분짜리 실천 과제, 1문장)
        7) 🎯 다음 액션 2: (문제풀이/복습/코드작성 중 하나로, 1문장)

        [작성 규칙]
        - 총 7줄 고정, 각 줄은 35~70자 정도로 '구체적으로' 작성 (너무 짧게 금지)
        - "좋습니다/열심히" 같은 뭉뚱그린 칭찬만으로 끝내지 말 것
        - testScore가 있으면 점수에 따라 톤 조절:
          * 90점 이상: 응용/예외케이스/리팩토링 같은 고급 제안 포함
          * 70~89점: 개념+응용 균형, 실수 줄이는 체크리스트 제안
          * 69점 이하: 핵심 개념 1~2개 재정리 + 짧은 반복 연습 제안
        - studentFeedback이 null/빈값이면 "학생 소감 없음"으로 간주하고 그걸 억지로 해석하지 말 것
        - 코드블록/JSON/마크다운 제목 금지, 텍스트만 출력

        [학습 로그]
        planId: %s
        dayCount: %s
        contentSummary: %s
        dailySummary: %s
        testScore: %s
        studentFeedback: %s
        """,
                String.valueOf(log.getPlanId()),
                String.valueOf(log.getDayCount()),
                String.valueOf(log.getContentSummary()),
                String.valueOf(log.getDailySummary()),
                String.valueOf(log.getTestScore()),
                String.valueOf(log.getStudentFeedback())
        );

        String res = chatModel.call(prompt);
        return cleanText(res);
    }

    private String cleanText(String text) {
        if (text == null) return "";
        String cleaned = text.trim();
        // 가끔 ``` 로 감싸서 오면 제거
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "");
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }


    // --- Redis 세션 관리 ---
    public void saveSessionState(Long planId, String stateJson) {
        String key = "session:" + planId;
        redisTemplate.opsForValue().set(key, stateJson, 24, TimeUnit.HOURS);
    }

    public String getSessionState(Long planId) {
        return redisTemplate.opsForValue().get("session:" + planId);
    }

    public void clearSessionState(Long planId) {
        redisTemplate.delete("session:" + planId);
    }
}