package com.tutoroo.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // --- [Step 2] 수준 파악 상담 (이번 목표에 대한 전용 테스트) ---
    public AssessmentDTO.ConsultResponse proceedConsultation(AssessmentDTO.ConsultRequest request) {
        // 대화가 10턴 이상 길어지면 강제 종료 (비용/피로도 관리)
        if (request.history() != null && request.history().size() > 10) {
            return finishConsultation("정보가 충분히 모였습니다. 이제 분석을 바탕으로 로드맵을 생성하겠습니다!");
        }

        // DB에서 시스템 프롬프트 로드 (없으면 기본값)
        String basePrompt = commonMapper.findPromptContentByKey("CONSULT_SYSTEM");
        if (basePrompt == null) basePrompt = "너는 꼼꼼한 학습 컨설턴트야.";

        // AI에게 '평가자' 역할을 부여하는 강력한 프롬프트
        String prompt = String.format("""
                %s
                
                [현재 학습 요청 정보]
                - 목표: %s
                - 기한: %s
                - 하루 공부 시간: %s
                
                [지시사항]
                너의 목표는 이 학생이 위 목표(%s)를 달성하기 위해 현재 어느 정도 수준인지(기초/중급/고급)를 파악하는 거야.
                사용자에게 **수준을 테스트할 수 있는 질문**을 하나만 해.
                개념을 묻거나, 경험을 묻거나, 예시 코드를 보여주고 해석하게 하는 등 구체적으로 질문해.
                
                만약 충분히 파악되었다고 판단되면 답변 맨 끝에 "[FINISH]"를 붙여.
                """,
                basePrompt,
                request.studyInfo().goal(),
                request.studyInfo().deadline(),
                request.studyInfo().availableTime(),
                request.studyInfo().goal()
        );

        String fullPrompt = buildConversationPrompt(prompt, request.history(), request.lastUserMessage());
        String aiResponse = chatModel.call(fullPrompt);

        // 종료 신호 감지
        if (aiResponse.contains("[FINISH]")) {
            String finalMsg = aiResponse.replace("[FINISH]", "").trim();
            if (finalMsg.isEmpty()) finalMsg = "수준 파악이 완료되었습니다. 로드맵을 생성합니다.";
            return finishConsultation(finalMsg);
        }

        return AssessmentDTO.ConsultResponse.builder()
                .aiMessage(aiResponse)
                .audioUrl(generateTtsAudio(aiResponse))
                .isFinished(false)
                .build();
    }

    // --- [Step 3] 로드맵 생성 (빙산의 일각 + 진짜 빙산) ---
    @Transactional
    public AssessmentDTO.AssessmentResultResponse analyzeAndCreateRoadmap(Long userId, AssessmentDTO.AssessmentSubmitRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        // [핵심 추가] 멤버십 등급별 플랜 생성 제한 강제 검증
        checkPlanLimit(user);

        // 1. 레벨 분석 (상담 내역 기반)
        String analysisJson = analyzeStudentLevel(user, request.studyInfo(), request.history());
        AnalysisResult analysis;
        try {
            analysis = objectMapper.readValue(analysisJson, AnalysisResult.class);
        } catch (Exception e) {
            log.error("분석 파싱 실패: {}", e.getMessage());
            analysis = new AnalysisResult("BEGINNER", "상담 데이터 부족으로 기초부터 시작합니다.");
        }

        // 2. [핵심] 전체 로드맵 생성 (목차형 + 상세형 분리 생성)
        String roadmapJson = generateFullRoadmap(user, request.studyInfo(), analysis);
        AssessmentDTO.RoadmapData roadmapData;
        try {
            roadmapData = objectMapper.readValue(roadmapJson, AssessmentDTO.RoadmapData.class);
        } catch (Exception e) {
            log.error("로드맵 파싱 실패. Raw JSON: {}", roadmapJson);
            throw new TutorooException("로드맵 생성 형식이 올바르지 않습니다.", ErrorCode.AI_PROCESSING_ERROR);
        }

        // 3. DB 저장 (전체 데이터 포함)
        savePlanToDB(userId, request.studyInfo(), roadmapJson, analysis.level);

        // 4. 응답 생성 (화면에는 '빙산의 일각'인 Overview만 전달)
        AssessmentDTO.RoadmapOverview overview = AssessmentDTO.RoadmapOverview.builder()
                .summary(roadmapData.summary())
                .chapters(roadmapData.tableOfContents()) // 목차만 전달
                .build();

        return AssessmentDTO.AssessmentResultResponse.builder()
                .planId(null) // 필요 시 savePlanToDB 수정하여 ID 반환 가능
                .analyzedLevel(analysis.level)
                .analysisReport(analysis.report)
                .overview(overview)
                .message("로드맵 생성이 완료되었습니다. 대시보드에서 상세 내용을 확인하세요.")
                .build();
    }

    // --- [복구됨] 간편 생성 (StudyController 호환용) ---
    // StudyController.createStudyPlan에서 호출하는 메서드입니다.
    @Transactional
    public AssessmentDTO.RoadmapResponse createStudentRoadmap(Long userId, AssessmentDTO.RoadmapRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        // [핵심 추가] 멤버십 등급별 플랜 생성 제한 강제 검증 (간편 생성도 막아야 함)
        checkPlanLimit(user);

        // 1. 입력 정보 변환 (간편 생성이라 상담 내역 없음)
        String level = request.currentLevel() != null ? request.currentLevel() : "BEGINNER";
        AssessmentDTO.StudyStartRequest info = new AssessmentDTO.StudyStartRequest(
                request.goal(), "3개월", "2시간", request.teacherType()
        );
        AnalysisResult analysis = new AnalysisResult(level, "간편 생성을 통해 생성된 로드맵입니다.");

        // 2. 전체 로드맵 생성 (대시보드 호환을 위해 상세 데이터도 생성)
        String roadmapJson = generateFullRoadmap(user, info, analysis);
        AssessmentDTO.RoadmapData roadmapData;
        try {
            roadmapData = objectMapper.readValue(roadmapJson, AssessmentDTO.RoadmapData.class);
        } catch (Exception e) {
            log.error("로드맵 파싱 실패: {}", e.getMessage());
            throw new TutorooException(ErrorCode.AI_PROCESSING_ERROR);
        }

        // 3. DB 저장
        savePlanToDB(userId, info, roadmapJson, level);

        // 4. 컨트롤러 반환 타입(RoadmapResponse)에 맞춰 데이터 변환
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

    // --- [재생성] 기존 플랜 수정 ---
    @Transactional
    public AssessmentDTO.AssessmentResultResponse regenerateRoadmap(Long userId, Long planId, AssessmentDTO.AssessmentSubmitRequest request) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);
        if (!plan.getUserId().equals(userId)) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        // 재생성은 기존 플랜을 대체하는 것이지만, 현재 로직상 새 플랜을 생성하므로 제한 체크가 필요할 수 있음
        // 만약 '기존 플랜 업데이트'로 로직을 바꾼다면 체크를 뺄 수 있으나, 안전을 위해 analyzeAndCreateRoadmap 호출 유지
        return analyzeAndCreateRoadmap(userId, request);
    }

    // --- 기타 기능 (레벨 테스트 등 - 기존 유지) ---
    public AssessmentDTO.LevelTestResponse generateLevelTest(AssessmentDTO.LevelTestRequest request) {
        String prompt = String.format("과목: %s. 5지선다 5문제 JSON 출제.", request.subject());
        String json = cleanJson(chatModel.call(prompt));
        try {
            List<AssessmentDTO.LevelTestResponse.TestQuestion> qs = objectMapper.readValue(json, new TypeReference<>() {});
            return AssessmentDTO.LevelTestResponse.builder().testId(UUID.randomUUID().toString()).subject(request.subject()).questions(qs).build();
        } catch(Exception e) {
            return AssessmentDTO.LevelTestResponse.builder().testId("error").questions(new ArrayList<>()).build();
        }
    }

    public AssessmentDTO.AssessmentResult evaluateLevelTest(Long userId, AssessmentDTO.TestSubmitRequest request) {
        return AssessmentDTO.AssessmentResult.builder().level("BEGINNER").score(0).analysis("기본 제공").recommendedPath("기초").build();
    }

    // --- Private Methods (Prompt Logic) ---

    // [New] 플랜 생성 제한 검증 헬퍼
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

    // DB 저장 헬퍼
    private void savePlanToDB(Long userId, AssessmentDTO.StudyStartRequest info, String json, String level) {
        StudyPlanEntity plan = StudyPlanEntity.builder()
                .userId(userId)
                .goal(info.goal())
                .persona(info.teacherType() != null ? info.teacherType() : "TIGER")
                .roadmapJson(json)
                .progressRate(0.0)
                .status("PROCEEDING")
                .currentLevel(level)
                .isPaid(false)
                .build();
        studyMapper.savePlan(plan);
    }

    // 1. 레벨 분석 프롬프트
    private String analyzeStudentLevel(UserEntity user, AssessmentDTO.StudyStartRequest info, List<AssessmentDTO.Message> history) {
        String prompt = String.format("""
                [학생 프로필] %s (%d세)
                [목표] %s (기한: %s)
                [상담 내역]
                %s
                
                위 상담 내용을 바탕으로 이 학생이 목표를 달성하기 위한 현재 수준(BEGINNER/INTERMEDIATE/ADVANCED)을 냉철하게 분석해.
                JSON으로 출력: {"level": "...", "report": "..."}
                """, user.getName(), user.getAge(), info.goal(), info.deadline(), serializeHistory(history));

        return cleanJson(chatModel.call(prompt));
    }

    // 2. [핵심] 전체 로드맵 생성 프롬프트 (목차 vs 상세 분리)
    private String generateFullRoadmap(UserEntity user, AssessmentDTO.StudyStartRequest info, AnalysisResult analysis) {
        String prompt = String.format("""
                [학생 정보] 이름: %s (%d세), 목표: %s, 기한: %s, 시간: %s
                [분석 결과] 수준: %s, 코멘트: %s
                
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
                analysis.level, analysis.report
        );

        return cleanJson(chatModel.call(prompt));
    }

    private String buildConversationPrompt(String base, List<AssessmentDTO.Message> history, String lastMsg) {
        StringBuilder sb = new StringBuilder(base).append("\n[대화 내역]\n").append(serializeHistory(history));
        if (lastMsg != null) sb.append("User: ").append(lastMsg);
        return sb.toString();
    }

    private String serializeHistory(List<AssessmentDTO.Message> history) {
        if (history == null) return "";
        StringBuilder sb = new StringBuilder();
        history.forEach(m -> sb.append(m.role()).append(": ").append(m.content()).append("\n"));
        return sb.toString();
    }

    private AssessmentDTO.ConsultResponse finishConsultation(String msg) {
        return AssessmentDTO.ConsultResponse.builder().aiMessage(msg).audioUrl(generateTtsAudio(msg)).isFinished(true).build();
    }

    private String generateTtsAudio(String text) {
        try {
            SpeechResponse response = speechModel.call(new SpeechPrompt(text));
            return fileStore.storeFile(response.getResult().getOutput(), ".mp3");
        } catch (Exception e) { return null; }
    }

    private String cleanJson(String text) {
        if (text.startsWith("```")) return text.replaceAll("^```json", "").replaceAll("^```", "").trim();
        return text;
    }

    private record AnalysisResult(String level, String report) {}
}