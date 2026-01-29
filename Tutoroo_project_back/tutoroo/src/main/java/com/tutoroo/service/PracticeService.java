package com.tutoroo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.PracticeDTO;
import com.tutoroo.entity.*;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.PracticeMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeMapper practiceMapper;
    private final StudyMapper studyMapper;
    private final OpenAiChatModel chatModel;
    private final ImageModel imageModel;
    private final FileStore fileStore;
    private final ObjectMapper objectMapper;

    // =================================================================================
    // 1. 무한 실전 테스트 생성 (이미지 생성 & DB 저장 탑재)
    // =================================================================================
    @Transactional
    public PracticeDTO.TestResponse generatePracticeTest(Long userId, PracticeDTO.GenerateRequest request) {
        StudyPlanEntity plan = studyMapper.findById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        // 1. 약점 분석 기반 토픽 선정
        String topicInstruction = "전체 학습 범위에서 중요 개념 위주로 출제해.";
        if (request.isWeaknessMode()) {
            List<String> weakTopics = practiceMapper.findTopWeakTopics(userId, request.planId());
            if (!weakTopics.isEmpty()) {
                topicInstruction = "학생이 자주 틀리는 다음 주제를 집중 공략해: " + String.join(", ", weakTopics);
            }
        }

        // 2. AI 프롬프트 (시각 자료 요청 포함)
        String promptText = String.format("""
                [Role] You are a professional exam creator for '%s'.
                [Topic] %s
                [Difficulty] %s (1=Easy, 5=Hard)
                [Count] Create %d high-quality questions.
                
                [Requirements]
                1. **Question Types**: Mix MULTIPLE_CHOICE, SHORT_ANSWER, and VISUAL_ANALYSIS.
                2. **Visuals**: If a question needs an image (e.g., Geometry, Art, Biology), provide an 'imagePrompt' field.
                   - Example: "A diagram of a mitochondria" or "A painting in Impressionist style".
                3. **Language**: The content MUST be in Korean (한국어).
                
                [JSON Format Strict]
                [
                    {
                        "topic": "Topic Name",
                        "type": "MULTIPLE_CHOICE",
                        "question": "Question text...",
                        "options": ["Option A", "B", "C", "D"],
                        "answer": "Correct Answer",
                        "explanation": "Detailed explanation...",
                        "imagePrompt": "Description for DALL-E (or null if not needed)"
                    }
                ]
                """, plan.getGoal(), topicInstruction, request.difficulty(), request.questionCount());

        // 3. 문제 생성 및 파싱 (재시도 로직 포함)
        List<Map<String, Object>> rawQuestions = generateAndParseQuestionsWithRetry(promptText);

        List<PracticeDTO.PracticeQuestion> responseList = new ArrayList<>();

        for (Map<String, Object> raw : rawQuestions) {
            String questionText = (String) raw.get("question");
            String contentHash = hashString(questionText);

            // [중복 방지]
            if (practiceMapper.countByContentHash(contentHash) > 0) {
                log.info("중복 문제 패스: {}", raw.get("topic"));
                continue;
            }

            // [이미지 생성] DALL-E 호출
            String imageUrl = null;
            String imagePrompt = (String) raw.get("imagePrompt");
            if (StringUtils.hasText(imagePrompt) && !imagePrompt.equalsIgnoreCase("null")) {
                try {
                    imageUrl = generateQuestionImage(imagePrompt);
                } catch (Exception e) {
                    log.warn("이미지 생성 실패 (텍스트로만 저장): {}", e.getMessage());
                }
            }

            // [DB 저장] Entity 빌드 (imageUrl 포함)
            PracticeQuestionEntity entity = PracticeQuestionEntity.builder()
                    .planId(plan.getId())
                    .contentHash(contentHash)
                    .questionJson(toJson(raw)) // 원본 데이터 보존
                    .topic((String) raw.get("topic"))
                    .questionType((String) raw.get("type"))
                    .difficulty(parseDifficulty(request.difficulty()))
                    .imageUrl(imageUrl) // [핵심] 생성된 이미지 URL을 DB에 영구 저장
                    .build();

            practiceMapper.saveQuestion(entity);

            // [응답] DTO 변환
            responseList.add(PracticeDTO.PracticeQuestion.builder()
                    .questionId(entity.getId())
                    .topic(entity.getTopic())
                    .type(QuestionType.valueOf(entity.getQuestionType()))
                    .questionText(questionText)
                    .options((List<String>) raw.get("options"))
                    .referenceMediaUrl(imageUrl) // 프론트엔드에 이미지 전달
                    .build());
        }

        return PracticeDTO.TestResponse.builder()
                .testSessionId(System.currentTimeMillis())
                .questions(responseList)
                .build();
    }

    // =================================================================================
    // 2. 채점 및 정밀 해설
    // =================================================================================
    @Transactional
    public PracticeDTO.GradingResponse submitPracticeTest(Long userId, PracticeDTO.SubmitRequest request) {
        List<PracticeDTO.GradingResponse.QuestionResult> results = new ArrayList<>();
        int correctCount = 0;

        for (PracticeDTO.SubmitRequest.AnswerSubmission ans : request.answers()) {
            PracticeQuestionEntity question = practiceMapper.findQuestionById(ans.questionId());
            if (question == null) continue;

            String prompt = String.format("""
                    [채점]
                    문제: %s
                    학생 답안: %s
                    
                    1. 정답 여부(true/false)
                    2. 명쾌한 해설 (학생이 이해하기 쉽게)
                    3. 핵심 태그 (약점 분석용 단어 1개)
                    
                    JSON: {"isCorrect": boolean, "explanation": "...", "tag": "..."}
                    """, extractQuestionText(question.getQuestionJson()), ans.answerText());

            Map<String, Object> aiResult = generateAndParseSingleResultWithRetry(prompt);

            boolean isCorrect = (boolean) aiResult.getOrDefault("isCorrect", false);
            if (isCorrect) correctCount++;

            // 로그 저장
            PracticeLogEntity log = PracticeLogEntity.builder()
                    .userId(userId)
                    .questionId(question.getId())
                    .userAnswer(ans.answerText())
                    .isCorrect(isCorrect)
                    .aiFeedback((String) aiResult.get("explanation"))
                    .build();
            practiceMapper.saveLog(log);

            results.add(new PracticeDTO.GradingResponse.QuestionResult(
                    question.getId(),
                    isCorrect,
                    ans.answerText(),
                    (String) aiResult.get("explanation"),
                    (String) aiResult.get("tag")
            ));
        }

        int totalScore = (request.answers().isEmpty()) ? 0 : (int)(((double)correctCount / request.answers().size()) * 100);

        return PracticeDTO.GradingResponse.builder()
                .totalScore(totalScore)
                .summaryReview(totalScore >= 80 ? "완벽합니다! 다음 단계로 넘어가도 좋겠어요." : "오답 노트를 꼭 확인하고 복습하세요.")
                .results(results)
                .build();
    }

    // =================================================================================
    // 3. 오답 클리닉 (통계 + 복습 추천)
    // =================================================================================
    @Transactional(readOnly = true)
    public PracticeDTO.WeaknessAnalysisResponse getWeaknessAnalysis(Long userId, Long planId) {
        List<String> weakTopics = practiceMapper.findTopWeakTopics(userId, planId);
        List<PracticeDTO.WeaknessAnalysisResponse.WeakPoint> weakPoints = new ArrayList<>();
        List<PracticeDTO.PracticeQuestion> recommended = new ArrayList<>();

        for (String topic : weakTopics) {
            // 통계 (실제로는 DB count 쿼리가 더 정확함, 여기선 예시값)
            weakPoints.add(new PracticeDTO.WeaknessAnalysisResponse.WeakPoint(topic, 0, 0.0));

            // 복습용 문제 추출 (과거 틀린 문제 재사용)
            List<PracticeQuestionEntity> wrongs = practiceMapper.findWrongQuestionsByTopic(userId, topic);
            for (PracticeQuestionEntity q : wrongs) {
                Map<String, Object> raw = parseSingleResult(q.getQuestionJson());

                recommended.add(PracticeDTO.PracticeQuestion.builder()
                        .questionId(q.getId())
                        .topic(q.getTopic())
                        .type(QuestionType.valueOf(q.getQuestionType()))
                        .questionText((String) raw.get("question"))
                        .options((List<String>) raw.get("options"))
                        // [최적화] DB에 저장된 이미지 URL을 바로 사용 (DALL-E 호출 X -> 비용 절감)
                        .referenceMediaUrl(q.getImageUrl())
                        .build());
            }
        }
        return PracticeDTO.WeaknessAnalysisResponse.builder()
                .weakPoints(weakPoints)
                .recommendedQuestions(recommended)
                .build();
    }

    // =========================================================================
    // [Private Helpers]
    // =========================================================================

    // 1. 이미지 생성 (DALL-E 3 -> Local/S3 저장)
    private String generateQuestionImage(String prompt) {
        ImageResponse response = imageModel.call(new ImagePrompt(
                "Education diagram, clear, minimalist style: " + prompt,
                // [수정] OpenAI 전용 옵션 사용 및 Base64 응답 요청
                OpenAiImageOptions.builder()
                        .withModel("dall-e-3")
                        .withHeight(1024)
                        .withWidth(1024)
                        .withResponseFormat("b64_json") // Base64로 받아야 파일 저장이 빠름
                        .build()
        ));

        // [수정] mb64()가 아니라 getB64Json() 사용
        String base64 = response.getResult().getOutput().getB64Json();

        if (base64 != null) {
            byte[] decoded = Base64.getDecoder().decode(base64);
            return fileStore.storeFile(decoded, ".png");
        }
        return null;
    }

    // 2. 안전한 JSON 파싱 (재시도 로직)
    private List<Map<String, Object>> generateAndParseQuestionsWithRetry(String prompt) {
        for (int i = 0; i < 2; i++) { // 최대 2회 시도
            try {
                String json = cleanJson(chatModel.call(prompt));
                return objectMapper.readValue(json, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("JSON 파싱 실패 (재시도 {}회): {}", i + 1, e.getMessage());
            }
        }
        return new ArrayList<>(); // 실패 시 빈 리스트 (서버 에러 방지)
    }

    private Map<String, Object> generateAndParseSingleResultWithRetry(String prompt) {
        for (int i = 0; i < 2; i++) {
            try {
                String json = cleanJson(chatModel.call(prompt));
                return objectMapper.readValue(json, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("채점 결과 파싱 실패: {}", e.getMessage());
            }
        }
        return Map.of("isCorrect", false, "explanation", "채점 시스템 오류로 오답 처리되었습니다.", "tag", "Error");
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    private String extractQuestionText(String json) {
        Map<String, Object> map = parseSingleResult(json);
        return (String) map.getOrDefault("question", "");
    }

    private Map<String, Object> parseSingleResult(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return Map.of(); }
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (Exception e) { return String.valueOf(input.hashCode()); }
    }

    private int parseDifficulty(String diff) {
        if ("EASY".equalsIgnoreCase(diff)) return 1;
        if ("HARD".equalsIgnoreCase(diff)) return 5;
        return 3;
    }
}