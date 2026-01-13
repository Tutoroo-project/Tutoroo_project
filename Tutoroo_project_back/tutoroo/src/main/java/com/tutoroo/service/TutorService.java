package com.tutoroo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.TutorDTO;
import com.tutoroo.entity.*;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.CommonMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
// [수정된 부분] 올바른 패키지 경로 (openai 제거)
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorService {

    private final OpenAiChatModel chatModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiImageModel imageModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel; // STT 모델

    private final StudyMapper studyMapper;
    private final UserMapper userMapper;
    private final CommonMapper commonMapper;
    private final ObjectMapper objectMapper;

    // --- [신규 기능 1] STT: 음성을 텍스트로 변환 (실시간 대화용) ---
    @Transactional(readOnly = true)
    public String convertSpeechToText(MultipartFile audioFile) {
        try {
            Resource audioResource = new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return "speech.mp3"; // OpenAI API는 파일명을 요구함
                }
            };

            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource);
            AudioTranscriptionResponse response = transcriptionModel.call(prompt);
            return response.getResult().getOutput();
        } catch (Exception e) {
            log.error("STT Conversion Failed: {}", e.getMessage());
            throw new TutorooException("음성 인식 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 1. 수업 시작
    @Transactional(readOnly = true)
    public TutorDTO.ClassStartResponse startClass(Long userId, TutorDTO.ClassStartRequest request) {
        UserEntity user = userMapper.findById(userId);
        StudyPlanEntity plan = studyMapper.findPlanById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String topic = request.dayCount() + "일차 수업";

        StudyLogEntity lastLog = studyMapper.findLatestLogByPlanId(plan.getId());
        StringBuilder context = new StringBuilder();
        if (lastLog != null) {
            context.append("이전 점수: ").append(lastLog.getTestScore()).append("점. ");
            if (lastLog.getStudentFeedback() != null) context.append("학생 요청: ").append(lastLog.getStudentFeedback());
        }
        if (request.dailyMood() != null) context.append(" (오늘 기분: ").append(request.dailyMood()).append(")");

        String template = getPromptTemplate("CLASS_START");
        String prompt = String.format(template, plan.getPersona(), context, request.dayCount(), topic);

        String aiMessage = chatModel.call(prompt);
        String audioBase64 = generateTieredTtsWithCache(aiMessage, user.getEffectiveTier());
        String imageUrl = generateImage(topic + " concept illustration, simple vector style");

        return new TutorDTO.ClassStartResponse(topic, aiMessage, audioBase64, imageUrl, null, 25, 5);
    }

    // 2. 데일리 테스트 생성
    @Transactional(readOnly = true)
    public TutorDTO.DailyTestResponse generateTest(Long userId, Long planId, int dayCount) {
        StudyPlanEntity plan = studyMapper.findPlanById(planId);
        String template = getPromptTemplate("TEST_GENERATE");
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format(template, plan.getGoal(), dayCount));

        if (dayCount > 1) {
            StudyLogEntity lastLog = studyMapper.findLatestLogByPlanId(planId);
            if (lastLog != null && lastLog.getDailySummary() != null) {
                promptBuilder.append("\n\n[추가 지시사항] 반드시 지난 시간 복습 문제도 1개 포함해서, 총 2문제를 내줘.");
                promptBuilder.append("\n지난 학습 요약: ").append(lastLog.getDailySummary());
            }
        }

        String question = chatModel.call(promptBuilder.toString());
        String imageUrl = generateImage("Diagram for quiz: " + question);

        return new TutorDTO.DailyTestResponse("QUIZ", question, imageUrl, null, 300);
    }

    // 3. 테스트 제출 및 채점 (+ 스트릭 업데이트)
    public TutorDTO.TestFeedbackResponse submitTest(Long userId, Long planId, String textAnswer, MultipartFile image) {
        UserEntity user = userMapper.findById(userId);

        String input = textAnswer + (image != null ? " [이미지 답안]" : "");
        String template = getPromptTemplate("TEST_FEEDBACK");
        String prompt = String.format(template, input);

        String aiResponse = chatModel.call(prompt);

        int score = parseScore(aiResponse);
        String summary = aiResponse.contains("★") ? aiResponse.substring(aiResponse.indexOf("★")) : "";
        int attitudePoint = evaluateAttitude(textAnswer);

        int totalPointChange = ((score >= 80) ? 100 : (score <= 40 ? -50 : 0)) + attitudePoint;
        if (totalPointChange != 0) userMapper.updateUserPointByPlan(planId, totalPointChange);

        boolean isPassed = score >= 60;
        studyMapper.saveLog(StudyLogEntity.builder()
                .planId(planId)
                .dayCount(getCurrentDayCount(planId))
                .testScore(score)
                .aiFeedback(aiResponse)
                .dailySummary(summary)
                .pointChange(totalPointChange)
                .isCompleted(isPassed)
                .build());

        // [신규 기능 2] 스트릭(Streak) 업데이트 - 오늘 처음 공부했다면 잔디 심기
        updateStreak(user);

        String audioBase64 = generateTieredTtsWithCache(aiResponse.substring(0, Math.min(aiResponse.length(), 200)), user.getEffectiveTier());
        String expImage = (score < 100) ? generateImage("Explanation: " + summary) : null;

        return new TutorDTO.TestFeedbackResponse(score, aiResponse, summary, audioBase64, expImage, null, isPassed);
    }

    // --- 스트릭 계산 로직 ---
    @Transactional
    protected void updateStreak(UserEntity user) {
        LocalDate today = LocalDate.now();
        LocalDate lastStudy = user.getLastStudyDate();

        if (lastStudy == null) {
            user.setCurrentStreak(1);
        } else if (lastStudy.equals(today.minusDays(1))) {
            // 어제 공부하고 오늘 또 함 -> 스트릭 +1
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else if (lastStudy.isBefore(today.minusDays(1))) {
            // 하루 이상 빼먹음 -> 스트릭 초기화
            user.setCurrentStreak(1);
        }
        // 오늘 이미 공부했으면(lastStudy == today) 아무것도 안 함

        user.setLastStudyDate(today);
        userMapper.update(user); // 변경사항 DB 반영
        log.info("User {} Streak updated: {}", user.getUsername(), user.getCurrentStreak());
    }

    // [Step 20 보조] 태도 분석 메서드
    private int evaluateAttitude(String answerText) {
        if (answerText == null || answerText.length() < 5) return 0;

        String prompt = String.format("""
            학생 답안: "%s"
            지시: 이 답안에서 학생의 학습 태도를 -10점(불량/욕설/무성의)에서 +10점(열정/성실/예의) 사이로 점수만 매겨.
            출력 예시: 5
            """, answerText);

        try {
            String response = chatModel.call(prompt).trim();
            return Integer.parseInt(response.replaceAll("[^0-9\\-]", ""));
        } catch (Exception e) {
            return 0; // 분석 실패 시 0점
        }
    }

    // 편의용 메서드
    private int getCurrentDayCount(Long planId) {
        StudyLogEntity last = studyMapper.findLatestLogByPlanId(planId);
        return (last == null) ? 1 : last.getDayCount();
    }

    // 4. 커리큘럼 조정 대화
    @Transactional(readOnly = true)
    public TutorDTO.FeedbackChatResponse adjustCurriculum(Long userId, Long planId, String message) {
        UserEntity user = userMapper.findById(userId);
        String aiResponse = chatModel.call(getPromptTemplate("Chat_FEEDBACK") + "\nUser: " + message);
        return new TutorDTO.FeedbackChatResponse(aiResponse, generateTieredTtsWithCache(aiResponse, user.getEffectiveTier()));
    }

    // 5. 선생님 평가 저장
    @Transactional
    public void saveStudentFeedback(TutorDTO.TutorReviewRequest request) {
        studyMapper.updateStudentFeedback(request.planId(), request.dayCount(), request.feedback());
    }

    // 6. 중간/기말고사 (Step 19)
    @Transactional(readOnly = true)
    public TutorDTO.ExamGenerateResponse generateExam(Long userId, Long planId, int startDay, int endDay) {
        List<StudyLogEntity> logs = studyMapper.findLogsBetweenDays(planId, startDay, endDay);
        if (logs.isEmpty()) throw new TutorooException("시험 범위에 해당하는 학습 기록이 없습니다.", ErrorCode.INVALID_INPUT_VALUE);

        String combinedSummary = logs.stream()
                .map(log -> log.getDayCount() + "일차: " + log.getDailySummary())
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
            자료: %s
            지시: 위 내용을 바탕으로 5개의 객관식 문제를 JSON 포맷으로 출제해.
            Format: {"title": "...", "questions": [{"number": 1, "question": "...", "options": ["..."]}]}
            """, combinedSummary);

        String jsonResponse = chatModel.call(prompt);
        try {
            if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.replaceAll("^```json", "").replaceAll("^```", "").replaceAll("```$", "").trim();
            }
            return objectMapper.readValue(jsonResponse, TutorDTO.ExamGenerateResponse.class);
        } catch (JsonProcessingException e) {
            throw new TutorooException(ErrorCode.AI_PROCESSING_ERROR);
        }
    }

    @Transactional
    public TutorDTO.ExamResultResponse submitExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        String prompt = "학생 답안: " + request.answers() + ". 5문제 채점 결과와 해설을 해줘.";
        String aiFeedback = chatModel.call(prompt);
        int score = parseScore(aiFeedback);

        int point = score * 5;
        userMapper.updateUserPointByPlan(request.planId(), point);

        return new TutorDTO.ExamResultResponse(score, (point > 200 ? 5 : 1), aiFeedback, new ArrayList<>(), score >= 60);
    }

    // --- Helper Methods ---
    private String getPromptTemplate(String key) {
        String content = commonMapper.findPromptContentByKey(key);
        return content != null ? content : "기본 프롬프트: " + key;
    }

    private String generateImage(String textPrompt) {
        try {
            return imageModel.call(new ImagePrompt(textPrompt,
                            OpenAiImageOptions.builder().withModel("dall-e-3").withHeight(1024).withWidth(1024).build()))
                    .getResult().getOutput().getUrl();
        } catch (Exception e) { return null; }
    }

    private String generateTieredTtsWithCache(String text, MembershipTier tier) {
        if (text == null || text.isEmpty()) return null;
        String voiceName = tier.getTtsVoice().toUpperCase();
        String textHash = generateHash(text + ":" + voiceName);
        TtsCacheEntity cached = commonMapper.findTtsCacheByHash(textHash);
        if (cached != null) return cached.getAudioBase64();
        String audio = callOpenAiTts(text, tier);
        if (audio != null) commonMapper.saveTtsCache(TtsCacheEntity.builder().textHash(textHash).audioBase64(audio).build());
        return audio;
    }

    private String callOpenAiTts(String text, MembershipTier tier) {
        try {
            OpenAiAudioApi.SpeechRequest.Voice voice = OpenAiAudioApi.SpeechRequest.Voice.ALLOY;
            try { voice = OpenAiAudioApi.SpeechRequest.Voice.valueOf(tier.getTtsVoice().toUpperCase()); } catch (Exception e) {}
            SpeechResponse res = speechModel.call(new SpeechPrompt(text, OpenAiAudioSpeechOptions.builder().model(tier.getTtsModel()).voice(voice).build()));
            return Base64.getEncoder().encodeToString(res.getResult().getOutput());
        } catch (Exception e) { return null; }
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) { return String.valueOf(input.hashCode()); }
    }

    private int parseScore(String text) {
        try {
            Matcher m = Pattern.compile("(점수|Score)\\s*:\\s*(\\d{1,3})").matcher(text);
            if (m.find()) return Integer.parseInt(m.group(2));
        } catch (Exception e) {}
        return 50;
    }
}