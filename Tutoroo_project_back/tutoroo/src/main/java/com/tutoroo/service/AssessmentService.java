package com.tutoroo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.AssessmentDTO;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.CommonMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final OpenAiChatModel chatModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final StudyMapper studyMapper;
    private final UserMapper userMapper;
    private final CommonMapper commonMapper;
    private final ObjectMapper objectMapper;
    private final FileStore fileStore;

    // [핵심 설정] 최소 상담 턴 수 10회 (심층 분석)
    private static final int MIN_CONSULT_TURNS = 10;

    // [유저 의도 감지] 조기 종료 키워드 (정규식)
    private static final Pattern STOP_KEYWORDS = Pattern.compile(".*(그만|종료|멈춰|끝|결과|로드맵|힘들|지겨|안할래|stop|finish|done).*", Pattern.CASE_INSENSITIVE);

    // --- [Step 2] 수준 파악 상담 (고도화됨) ---
    public AssessmentDTO.ConsultResponse proceedConsultation(AssessmentDTO.ConsultRequest request) {
        // 1. DB에서 기본 페르소나 로드
        String baseSystemPrompt = commonMapper.findPromptContentByKey("CONSULT_SYSTEM");
        if (baseSystemPrompt == null) {
            baseSystemPrompt = "너는 대한민국 최고의 입시/학습 컨설턴트야. 학생의 성적, 성향, 멘탈까지 완벽하게 파악해야 해.";
        }

        // 2. 현재 대화 턴 수 및 유저 의도 파악
        int currentTurnCount = (request.history() == null) ? 0 : request.history().size();
        String lastUserMessage = request.lastUserMessage();
        boolean userWantsToStop = isUserRequestingStop(lastUserMessage);

        // 3. [Dynamic Prompt] 상황에 맞는 프롬프트 조립
        String enhancedPrompt = buildGuardedPrompt(baseSystemPrompt, request, currentTurnCount, userWantsToStop);

        try {
            // 4. AI 호출
            String jsonResponse = chatModel.call(enhancedPrompt);
            String cleanedJson = cleanJson(jsonResponse);

            // 5. 응답 파싱
            JsonNode rootNode = objectMapper.readTree(cleanedJson);
            String message = rootNode.path("message").asText();
            boolean isFinished = rootNode.path("isFinished").asBoolean();

            // [최종 안전장치 Logic]
            // A. 유저가 멈추길 원하면 -> 무조건 종료 (AI가 눈치 없이 계속 질문하는 것 방지)
            if (userWantsToStop) {
                log.info("🛑 유저 요청으로 상담을 조기 종료합니다. (현재 턴: {})", currentTurnCount);
                isFinished = true;
                // 메시지가 너무 질문형이면 "네, 알겠습니다. 분석을 시작합니다." 등으로 덮어씌울 수도 있음
            }
            // B. 유저가 멈추길 원치 않는데, 10회 미만이고 AI가 끝내려 하면 -> 강제 연장
            else if (isFinished && currentTurnCount < MIN_CONSULT_TURNS) {
                log.info("⚠️ 심층 분석을 위해 상담을 강제로 연장합니다. (현재 턴: {} < {})", currentTurnCount, MIN_CONSULT_TURNS);
                isFinished = false;
            }

            // 6. TTS 생성
            String audioUrl = generateTtsAudio(message);

            return AssessmentDTO.ConsultResponse.builder()
                    .aiMessage(message)
                    .audioUrl(audioUrl)
                    .isFinished(isFinished)
                    .build();

        } catch (Exception e) {
            log.error("Consultation Error: ", e);
            // 에러 발생 시 안전하게 종료 처리하지 않고 예외를 던져 프론트가 알게 함
            throw new TutorooException("상담 진행 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // --- [Step 3] 로드맵 생성 (분석 + 로드맵 + DB저장) ---
    @Transactional
    public AssessmentDTO.AssessmentResultResponse analyzeAndCreateRoadmap(Long userId, AssessmentDTO.AssessmentSubmitRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        // [검증] 멤버십 제한
        checkPlanLimit(user);

        // 1. 레벨 분석 (Current & Target)
        String analysisJson = analyzeStudentLevel(user, request.studyInfo(), request.history());
        AnalysisResult analysis;
        try {
            analysis = objectMapper.readValue(analysisJson, AnalysisResult.class);
        } catch (Exception e) {
            log.error("분석 파싱 실패: {}", e.getMessage());
            analysis = new AnalysisResult("BEGINNER", "INTERMEDIATE", "상담 데이터 부족으로 기초부터 시작합니다.");
        }

        // 2. 전체 로드맵 생성
        String roadmapJson = generateFullRoadmap(user, request.studyInfo(), analysis);
        AssessmentDTO.RoadmapData roadmapData;
        try {
            roadmapData = objectMapper.readValue(roadmapJson, AssessmentDTO.RoadmapData.class);
        } catch (Exception e) {
            log.error("로드맵 파싱 실패. Raw JSON: {}", roadmapJson);
            throw new TutorooException("로드맵 생성 형식이 올바르지 않습니다.", ErrorCode.AI_PROCESSING_ERROR);
        }

        // 3. DB 저장 (endDate, targetLevel 포함)
        savePlanToDB(userId, request.studyInfo(), roadmapJson, analysis);

        // 4. 응답 생성 (요약본)
        AssessmentDTO.RoadmapOverview overview = AssessmentDTO.RoadmapOverview.builder()
                .summary(roadmapData.summary())
                .chapters(roadmapData.tableOfContents())
                .build();

        return AssessmentDTO.AssessmentResultResponse.builder()
                .analyzedLevel(analysis.currentLevel)
                .analysisReport(analysis.analysisReport)
                .overview(overview)
                .message("로드맵 생성이 완료되었습니다. 대시보드에서 상세 내용을 확인하세요.")
                .build();
    }

    // --- [복구됨] 간편 생성 (StudyController 호환용) ---
    @Transactional
    public AssessmentDTO.RoadmapResponse createStudentRoadmap(Long userId, AssessmentDTO.RoadmapRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        checkPlanLimit(user);

        String currentLevel = request.currentLevel() != null ? request.currentLevel() : "BEGINNER";
        AssessmentDTO.StudyStartRequest info = new AssessmentDTO.StudyStartRequest(
                request.goal(), "3개월", "2시간", request.teacherType()
        );
        AnalysisResult analysis = new AnalysisResult(currentLevel, "ADVANCED", "간편 생성을 통해 생성된 로드맵입니다.");

        String roadmapJson = generateFullRoadmap(user, info, analysis);
        AssessmentDTO.RoadmapData roadmapData;
        try {
            roadmapData = objectMapper.readValue(roadmapJson, AssessmentDTO.RoadmapData.class);
        } catch (Exception e) {
            log.error("로드맵 파싱 실패: {}", e.getMessage());
            throw new TutorooException(ErrorCode.AI_PROCESSING_ERROR);
        }

        savePlanToDB(userId, info, roadmapJson, analysis);

        Map<String, String> simpleCurriculum = new HashMap<>();
        if (roadmapData.tableOfContents() != null) {
            for (AssessmentDTO.Chapter ch : roadmapData.tableOfContents()) {
                simpleCurriculum.put(ch.week(), ch.title() + ": " + ch.description());
            }
        }

        return AssessmentDTO.RoadmapResponse.builder()
                .summary(roadmapData.summary())
                .weeklyCurriculum(simpleCurriculum)
                .examSchedule(roadmapData.examSchedule())
                .build();
    }

    @Transactional
    public AssessmentDTO.AssessmentResultResponse regenerateRoadmap(Long userId, Long planId, AssessmentDTO.AssessmentSubmitRequest request) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);
        if (!plan.getUserId().equals(userId)) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return analyzeAndCreateRoadmap(userId, request);
    }

    // --- 기타 기능 (레벨 테스트 등) ---
    public AssessmentDTO.LevelTestResponse generateLevelTest(AssessmentDTO.LevelTestRequest request) {
        String prompt = String.format("과목: %s. 5지선다 5문제 JSON 출제.", request.subject());
        try {
            String json = cleanJson(chatModel.call(prompt));
            List<AssessmentDTO.LevelTestResponse.TestQuestion> qs = objectMapper.readValue(json, new TypeReference<>() {});
            return AssessmentDTO.LevelTestResponse.builder().testId(UUID.randomUUID().toString()).subject(request.subject()).questions(qs).build();
        } catch(Exception e) {
            return AssessmentDTO.LevelTestResponse.builder().testId("error").questions(new ArrayList<>()).build();
        }
    }

    public AssessmentDTO.AssessmentResult evaluateLevelTest(Long userId, AssessmentDTO.TestSubmitRequest request) {
        return AssessmentDTO.AssessmentResult.builder().level("BEGINNER").score(0).analysis("기본 제공").recommendedPath("기초").build();
    }

    // --- Private Helper Methods ---

    private boolean isUserRequestingStop(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        return STOP_KEYWORDS.matcher(message).find();
    }

    private String buildGuardedPrompt(String baseSystemPrompt, AssessmentDTO.ConsultRequest request, int currentTurn, boolean userWantsToStop) {
        StringBuilder sb = new StringBuilder();

        sb.append(baseSystemPrompt).append("\n\n");
        sb.append("You are a strict and highly detailed academic counselor. Your goal is to gather as much detail as possible about the student's current status, weaknesses, and habits.\n\n");

        sb.append("[Student Profile]\n");
        sb.append("Goal: ").append(request.studyInfo().goal()).append("\n");
        sb.append("Available Time: ").append(request.studyInfo().availableTime()).append("\n");
        sb.append("Deadline: ").append(request.studyInfo().deadline()).append("\n\n");

        sb.append("[SYSTEM RULES - EXECUTE STRICTLY]\n");
        sb.append("Current Turn: ").append(currentTurn).append(" / Target Min Turn: ").append(MIN_CONSULT_TURNS).append("\n");

        if (userWantsToStop) {
            sb.append("CONDITION: The student explicitly wants to stop or see the result.\n");
            sb.append("ACTION: Stop asking questions immediately. Provide a brief closing remark confirming you have analyzed their data.\n");
            sb.append("OUTPUT: Set 'isFinished': true.\n");
        } else if (currentTurn < MIN_CONSULT_TURNS) {
            sb.append("CONDITION: Conversation is in the early/middle stage (Under 10 turns).\n");
            sb.append("ACTION: You MUST NOT finish. Ask a deep, probing follow-up question. Dig into specific subjects, recent exam scores, or study distractions.\n");
            sb.append("EXAMPLE: 'mathematics score is low? which part? calculus or geometry?'\n");
            sb.append("OUTPUT: Set 'isFinished': false.\n");
        } else {
            sb.append("CONDITION: Sufficient data collected (Over 10 turns).\n");
            sb.append("ACTION: You may finish now. Summarize briefly and encourage the student.\n");
            sb.append("OUTPUT: Set 'isFinished': true.\n");
        }

        sb.append("6. FORMAT: Return ONLY JSON. Example: { \"message\": \"Your question here\", \"isFinished\": boolean }\n\n");

        sb.append("[Conversation History]\n");
        if (request.history() != null) {
            for (AssessmentDTO.Message msg : request.history()) {
                sb.append(msg.role()).append(": ").append(msg.content()).append("\n");
            }
        }

        if (request.lastUserMessage() != null && !request.lastUserMessage().isEmpty()) {
            sb.append("user: ").append(request.lastUserMessage()).append("\n");
        } else if (currentTurn == 0) {
            sb.append("system: Start the consultation with a sharp, insightful question based on their goal.\n");
        }

        return sb.toString();
    }

    private void checkPlanLimit(UserEntity user) {
        int currentActivePlans = studyMapper.countActivePlansByUserId(user.getId());
        int allowedLimit = user.getEffectiveTier().getMaxActiveGoals();

        if (currentActivePlans >= allowedLimit) {
            throw new TutorooException(
                    String.format("현재 등급(%s)에서는 더 이상 학습 목표를 생성할 수 없습니다. (최대 %d개)",
                            user.getEffectiveTier().name(), allowedLimit),
                    ErrorCode.MULTIPLE_PLANS_REQUIRED_PAYMENT
            );
        }
    }

    // [수정] AnalysisResult 파라미터 추가 및 필드 매핑 완벽 지원
    private void savePlanToDB(Long userId, AssessmentDTO.StudyStartRequest info, String json, AnalysisResult analysis) {
        // deadline 문자열 파싱 (예: "3개월", "100일") -> LocalDate
        LocalDate endDate = calculateEndDate(info.deadline());

        StudyPlanEntity plan = StudyPlanEntity.builder()
                .userId(userId)
                .goal(info.goal())
                .persona(info.teacherType() != null ? info.teacherType() : "TIGER")
                .roadmapJson(json)
                .progressRate(0.0)
                .status("PROCEEDING")
                .currentLevel(analysis.currentLevel)
                .targetLevel(analysis.targetLevel) // [New] 목표 레벨 저장
                .startDate(LocalDate.now())
                .endDate(endDate)                  // [New] 계산된 종료일 저장
                .isPaid(false)
                .build();
        studyMapper.savePlan(plan);
    }

    // [New] 종료일 계산 헬퍼
    private LocalDate calculateEndDate(String deadline) {
        if (deadline == null || deadline.isEmpty()) return LocalDate.now().plusDays(30);
        try {
            if (deadline.contains("개월")) {
                int months = Integer.parseInt(deadline.replaceAll("[^0-9]", ""));
                return LocalDate.now().plusMonths(months);
            } else if (deadline.contains("주")) {
                int weeks = Integer.parseInt(deadline.replaceAll("[^0-9]", ""));
                return LocalDate.now().plusWeeks(weeks);
            } else if (deadline.contains("일")) {
                int days = Integer.parseInt(deadline.replaceAll("[^0-9]", ""));
                return LocalDate.now().plusDays(days);
            }
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 기본값 30일 적용", deadline);
        }
        return LocalDate.now().plusDays(30);
    }

    private String analyzeStudentLevel(UserEntity user, AssessmentDTO.StudyStartRequest info, List<AssessmentDTO.Message> history) {
        String prompt = String.format("""
                [학생 프로필] %s (%d세)
                [목표] %s (기한: %s)
                [상담 내역]
                %s
                
                위 상담 내용을 바탕으로 이 학생의 '현재 수준'과 목표 달성을 위한 '목표 수준'을 냉철하게 분석해.
                JSON으로 출력 (Key 이름 준수):
                {
                  "currentLevel": "BEGINNER / INTERMEDIATE / ADVANCED 중 택1",
                  "targetLevel": "INTERMEDIATE / ADVANCED / MASTER 중 택1",
                  "analysisReport": "상세 분석 내용 (5문장 내외)"
                }
                """, user.getName(), user.getAge(), info.goal(), info.deadline(), serializeHistory(history));

        return cleanJson(chatModel.call(prompt));
    }

    private String generateFullRoadmap(UserEntity user, AssessmentDTO.StudyStartRequest info, AnalysisResult analysis) {
        String prompt = String.format("""
                [학생 정보] 이름: %s (%d세), 목표: %s, 기한: %s, 시간: %s
                [분석 결과] 수준: %s -> %s, 코멘트: %s
                
                위 정보를 종합하여 JSON을 작성해.
                
                1. **tableOfContents (빙산의 일각)**: 전체 과정을 주차별(또는 챕터별)로 요약한 목차. (title, description)
                2. **detailedCurriculum (진짜 빙산)**: 실제 일별 상세 학습 스케줄. Key는 '1주차' 등 목차와 일치해야 함.
                
                응답 형식 (JSON):
                {
                  "summary": "한 줄 슬로건",
                  "tableOfContents": [
                    {"week": "1주차", "title": "입문", "description": "기초 다지기"}
                  ],
                  "detailedCurriculum": {
                    "1주차": [
                      {"day": "1일차", "topic": "변수", "method": "강의", "material": "1장"},
                      {"day": "2일차", "topic": "연산자", "method": "실습", "material": "2장"}
                    ]
                  },
                  "examSchedule": ["2주차 테스트"]
                }
                """,
                user.getName(), user.getAge(), info.goal(), info.deadline(), info.availableTime(),
                analysis.currentLevel, analysis.targetLevel, analysis.analysisReport
        );

        return cleanJson(chatModel.call(prompt));
    }

    private String serializeHistory(List<AssessmentDTO.Message> history) {
        if (history == null) return "";
        StringBuilder sb = new StringBuilder();
        history.forEach(m -> sb.append(m.role()).append(": ").append(m.content()).append("\n"));
        return sb.toString();
    }

    private String generateTtsAudio(String text) {
        try {
            SpeechResponse response = speechModel.call(new SpeechPrompt(text));
            return fileStore.storeFile(response.getResult().getOutput(), ".mp3");
        } catch (Exception e) { return null; }
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    // [New] AnalysisResult 레코드 (targetLevel 추가)
    private record AnalysisResult(String currentLevel, String targetLevel, String analysisReport) {}
}