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

    // [설정] 최소 상담 턴 수
    private static final int MIN_CONSULT_TURNS = 10;
    // [설정] 상담 종료 키워드
    private static final Pattern STOP_KEYWORDS = Pattern.compile(".*(그만|종료|멈춰|끝|결과|로드맵|힘들|지겨|안할래|stop|finish|done|상담완료).*", Pattern.CASE_INSENSITIVE);

    // =================================================================================
    // 1. [상담] 수준 파악 티키타카 (DTO: ConsultRequest -> ConsultResponse)
    // =================================================================================

    public AssessmentDTO.ConsultResponse proceedConsultation(AssessmentDTO.ConsultRequest request) {
        // 턴 수 계산
        int currentTurnCount = (request.history() == null) ? 0 : (request.history().size() / 2) + 1;
        String lastUserMessage = request.lastUserMessage();
        boolean userWantsToStop = isUserRequestingStop(lastUserMessage);

        boolean isFinished = false;

        // 종료 조건 판단 logic
        if (currentTurnCount < MIN_CONSULT_TURNS) {
            // 강제 진행 (아직 정보 부족)
            isFinished = false;
            if (userWantsToStop) {
                lastUserMessage += " (System: User wants to stop, but you MUST politely insist on asking more details for better analysis.)";
            }
        } else {
            // 선택적 종료
            if (userWantsToStop) isFinished = true;
        }

        // 프롬프트 생성
        String systemPrompt = buildConsultationPrompt(request, currentTurnCount, isFinished);

        try {
            String aiMessage = chatModel.call(systemPrompt + "\n\nUser: " + lastUserMessage);

            if (isFinished) {
                aiMessage = "수고하셨습니다! 분석이 완료되었습니다. '결과 보기' 버튼을 눌러 상세 로드맵을 확인하세요.";
            }

            String audioUrl = generateTtsAudio(aiMessage);

            return AssessmentDTO.ConsultResponse.builder()
                    .aiMessage(aiMessage)
                    .audioUrl(audioUrl)
                    .isFinished(isFinished)
                    .build();

        } catch (Exception e) {
            log.error("Consultation Error", e);
            throw new TutorooException("상담 중 오류가 발생했습니다.", ErrorCode.AI_PROCESSING_ERROR);
        }
    }

    // =================================================================================
    // 2. [로드맵] 분석 및 생성 (DTO: AssessmentSubmitRequest -> AssessmentResultResponse)
    // =================================================================================

    @Transactional
    public AssessmentDTO.AssessmentResultResponse analyzeAndCreateRoadmap(Long userId, AssessmentDTO.AssessmentSubmitRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        checkPlanLimit(user);

        // 1. 레벨 분석 (내부 Record AnalysisResult 사용)
        String analysisJson = analyzeStudentLevel(user, request.studyInfo(), request.history());
        AnalysisResult analysis = parseAnalysisResult(analysisJson);

        // 2. 로드맵 생성 (JSON)
        String roadmapJson = generateFullRoadmap(user, request.studyInfo(), analysis);

        AssessmentDTO.RoadmapData roadmapData;
        try {
            roadmapData = objectMapper.readValue(roadmapJson, AssessmentDTO.RoadmapData.class);
        } catch (Exception e) {
            log.error("Roadmap Parsing Failed", e);
            roadmapData = createFallbackRoadmap(request.studyInfo().goal());
            try { roadmapJson = objectMapper.writeValueAsString(roadmapData); } catch(Exception ex){}
        }

        // 3. DB 저장 (StudyPlan)
        // DTO에 planId 필드가 없으므로, 여기서 생성된 ID를 반환 객체에 담아야 함
        Long planId = savePlanToDB(userId, request.studyInfo(), roadmapJson, analysis);

        // 4. 응답 DTO 생성
        return AssessmentDTO.AssessmentResultResponse.builder()
                .planId(planId)
                .analyzedLevel(analysis.currentLevel)
                .analysisReport(analysis.analysisReport)
                .overview(AssessmentDTO.RoadmapOverview.builder()
                        .summary(roadmapData.summary())
                        .chapters(roadmapData.tableOfContents())
                        .build())
                .message("로드맵 생성이 완료되었습니다.")
                .build();
    }

    // =================================================================================
    // 3. [간편 생성] (DTO: RoadmapRequest -> RoadmapResponse)
    // =================================================================================

    @Transactional
    public AssessmentDTO.RoadmapResponse createStudentRoadmap(Long userId, AssessmentDTO.RoadmapRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        checkPlanLimit(user);

        // 간편 생성을 위한 임시 정보 구성
        AssessmentDTO.StudyStartRequest info = new AssessmentDTO.StudyStartRequest(
                request.goal(), "4주", "60분", request.teacherType()
        );
        AnalysisResult analysis = new AnalysisResult(
                request.currentLevel() != null ? request.currentLevel() : "BEGINNER",
                "ADVANCED",
                "간편 진단 결과"
        );

        String roadmapJson = generateFullRoadmap(user, info, analysis);
        AssessmentDTO.RoadmapData data;
        try {
            data = objectMapper.readValue(roadmapJson, AssessmentDTO.RoadmapData.class);
        } catch (Exception e) {
            data = createFallbackRoadmap(request.goal());
        }

        savePlanToDB(userId, info, roadmapJson, analysis);

        // RoadmapResponse (간편 버전) 매핑
        Map<String, String> simpleCurriculum = new HashMap<>();
        if (data.tableOfContents() != null) {
            for (AssessmentDTO.Chapter ch : data.tableOfContents()) {
                simpleCurriculum.put(ch.week(), ch.title() + ": " + ch.description());
            }
        }

        return AssessmentDTO.RoadmapResponse.builder()
                .summary(data.summary())
                .weeklyCurriculum(simpleCurriculum)
                .examSchedule(data.examSchedule())
                .build();
    }

    // =================================================================================
    // 4. [기타] 레벨 테스트 및 로드맵 수정
    // =================================================================================

    public AssessmentDTO.LevelTestResponse generateLevelTest(AssessmentDTO.LevelTestRequest request) {
        String prompt = String.format("과목: %s. 5지선다 5문제를 JSON 배열로 출제해. [{\"questionNo\":1, \"question\":\"...\", \"options\":[\"...\"]}]", request.subject());
        try {
            String json = cleanJson(chatModel.call(prompt));
            List<AssessmentDTO.LevelTestResponse.TestQuestion> questions = objectMapper.readValue(json, new TypeReference<>() {});
            return AssessmentDTO.LevelTestResponse.builder()
                    .testId(UUID.randomUUID().toString())
                    .subject(request.subject())
                    .questions(questions)
                    .build();
        } catch (Exception e) {
            return AssessmentDTO.LevelTestResponse.builder().testId("error").questions(List.of()).build();
        }
    }

    public AssessmentDTO.AssessmentResult evaluateLevelTest(Long userId, AssessmentDTO.TestSubmitRequest request) {
        // 간단한 로직 (실제로는 정답 비교 필요)
        return AssessmentDTO.AssessmentResult.builder()
                .level("INTERMEDIATE")
                .score(80)
                .analysis("기초가 탄탄합니다.")
                .recommendedPath("심화 과정")
                .build();
    }

    @Transactional
    public void adjustRoadmap(Long planId, String feedback) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String prompt = String.format("Current JSON: %s\nFeedback: %s\nUpdate the JSON structure based on feedback.", plan.getRoadmapJson(), feedback);
        String json = cleanJson(chatModel.call(prompt));

        try {
            objectMapper.readTree(json);
            plan.setRoadmapJson(json);
            studyMapper.updatePlan(plan);
        } catch (Exception e) { log.error("Adjust Fail", e); }
    }

    // =================================================================================
    // 5. [Private Helpers] 프롬프트 및 유틸
    // =================================================================================

    private String buildConsultationPrompt(AssessmentDTO.ConsultRequest request, int currentTurn, boolean isFinished) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Academic Advisor. Goal: ").append(request.studyInfo().goal()).append("\n");
        sb.append("Available Time: ").append(request.studyInfo().availableTime()).append("\n");

        if (isFinished) {
            sb.append("ACTION: Summarize and finish consultation.");
        } else if (currentTurn < MIN_CONSULT_TURNS) {
            sb.append("ACTION: Ask one probing question to analyze weaknesses deeply. Do not finish yet.");
        } else {
            sb.append("ACTION: Gather any remaining details or suggest finishing.");
        }

        sb.append("\n[History]\n");
        if (request.history() != null) {
            request.history().forEach(m -> sb.append(m.role()).append(": ").append(m.content()).append("\n"));
        }
        return sb.toString();
    }

    private String analyzeStudentLevel(UserEntity user, AssessmentDTO.StudyStartRequest info, List<AssessmentDTO.Message> history) {
        String prompt = String.format("""
                Analyze student level based on chat history.
                Output JSON: {"currentLevel": "BEGINNER/INTERMEDIATE/ADVANCED", "targetLevel": "...", "analysisReport": "..."}
                User Goal: %s
                History: %s
                """, info.goal(), serializeHistory(history));
        return cleanJson(chatModel.call(prompt));
    }

    // [중요] DTO의 RoadmapData 구조(Map)와 일치하는 JSON 요청
    private String generateFullRoadmap(UserEntity user, AssessmentDTO.StudyStartRequest info, AnalysisResult analysis) {
        String prompt = String.format("""
                Create a 4-week roadmap JSON.
                Goal: %s, Level: %s -> %s
                
                [JSON Structure STRICTLY Match this]
                {
                  "summary": "String",
                  "tableOfContents": [
                    {"week": "1주차", "title": "Title", "description": "Desc"}
                  ],
                  "detailedCurriculum": {
                    "1주차": [
                      {"day": "1일차", "topic": "Topic", "method": "Lecture", "material": "Book"}
                    ]
                  },
                  "examSchedule": ["String"]
                }
                """, info.goal(), analysis.currentLevel, analysis.targetLevel);
        return cleanJson(chatModel.call(prompt));
    }

    private Long savePlanToDB(Long userId, AssessmentDTO.StudyStartRequest info, String json, AnalysisResult analysis) {
        StudyPlanEntity plan = StudyPlanEntity.builder()
                .userId(userId)
                .goal(info.goal())
                .persona("DEFAULT") // 학습 시작 시 선택
                .roadmapJson(json)
                .currentLevel(analysis.currentLevel)
                .targetLevel(analysis.targetLevel)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(28)) // 4주 고정
                .progressRate(0.0)
                .status("PROCEEDING")
                .isPaid(false)
                .build();
        studyMapper.savePlan(plan);
        return plan.getId();
    }

    private String serializeHistory(List<AssessmentDTO.Message> history) {
        if (history == null) return "";
        StringBuilder sb = new StringBuilder();
        history.forEach(m -> sb.append(m.role()).append(": ").append(m.content()).append("\n"));
        return sb.toString();
    }

    private boolean isUserRequestingStop(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        return STOP_KEYWORDS.matcher(message).find();
    }

    private void checkPlanLimit(UserEntity user) {
        if (studyMapper.countActivePlansByUserId(user.getId()) >= user.getEffectiveTier().getMaxActiveGoals()) {
            throw new TutorooException("Plan Limit Reached", ErrorCode.MULTIPLE_PLANS_REQUIRED_PAYMENT);
        }
    }

    private AnalysisResult parseAnalysisResult(String json) {
        try { return objectMapper.readValue(json, AnalysisResult.class); }
        catch (Exception e) { return new AnalysisResult("BEGINNER", "INTERMEDIATE", "분석 오류"); }
    }

    // DTO 구조에 맞춘 Fallback 데이터
    private AssessmentDTO.RoadmapData createFallbackRoadmap(String goal) {
        AssessmentDTO.Chapter ch = new AssessmentDTO.Chapter("1주차", "기초", "시작하기");
        AssessmentDTO.DailyDetail dd = new AssessmentDTO.DailyDetail("1일차", "개요", "읽기", "자료");
        return new AssessmentDTO.RoadmapData(
                goal + " 로드맵",
                List.of(ch),
                Map.of("1주차", List.of(dd)),
                List.of("테스트")
        );
    }

    private String generateTtsAudio(String text) {
        try {
            SpeechResponse res = speechModel.call(new SpeechPrompt(text));
            return fileStore.storeFile(res.getResult().getOutput(), ".mp3");
        } catch (Exception e) { return null; }
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }

    private record AnalysisResult(String currentLevel, String targetLevel, String analysisReport) {}
}