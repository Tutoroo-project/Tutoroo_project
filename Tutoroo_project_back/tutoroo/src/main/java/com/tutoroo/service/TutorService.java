package com.tutoroo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.TutorDTO;
import com.tutoroo.entity.*;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.CommonMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.util.FileStore;
import lombok.extern.slf4j.Slf4j;

// [Spring AI & Reactor Imports]
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TutorService {

    private final ChatClient chatClient;
    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiImageModel imageModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel;

    private final StudyMapper studyMapper;
    private final UserMapper userMapper;
    private final CommonMapper commonMapper;
    private final ObjectMapper objectMapper;
    private final FileStore fileStore;

    // 대화 기억을 위한 인메모리 저장소
    private final ChatMemory chatMemory = new InMemoryChatMemory();

    // 생성자 주입
    public TutorService(ChatClient.Builder chatClientBuilder,
                        OpenAiAudioSpeechModel speechModel,
                        OpenAiImageModel imageModel,
                        OpenAiAudioTranscriptionModel transcriptionModel,
                        StudyMapper studyMapper,
                        UserMapper userMapper,
                        CommonMapper commonMapper,
                        ObjectMapper objectMapper,
                        FileStore fileStore) {
        this.chatClient = chatClientBuilder
                .defaultSystem("당신은 Tutoroo의 AI 튜터입니다. 학생에게 친절하고 명확하게 설명해주세요.")
                .build();
        this.speechModel = speechModel;
        this.imageModel = imageModel;
        this.transcriptionModel = transcriptionModel;
        this.studyMapper = studyMapper;
        this.userMapper = userMapper;
        this.commonMapper = commonMapper;
        this.objectMapper = objectMapper;
        this.fileStore = fileStore;
    }

    // --- [Upgrade 3: 실시간 스트리밍 & 대화 기억] ---

    public Flux<String> chatWithTutorStream(Long userId, String message) {
        String conversationId = "user:" + userId;
        return chatClient.prompt()
                .user(message)
                .advisors(new MessageChatMemoryAdvisor(chatMemory, conversationId, 10))
                .stream()
                .content();
    }

    public void clearConversationMemory(Long userId) {
        chatMemory.clear("user:" + userId);
    }

    // --- [핵심 비즈니스 로직] ---

    // 1. 수업 시작
    @Transactional
    public TutorDTO.ClassStartResponse startClass(Long userId, TutorDTO.ClassStartRequest request) {
        UserEntity user = userMapper.findById(userId);
        StudyPlanEntity plan = studyMapper.findPlanById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String customName = (plan.getCustomTutorName() != null) ? plan.getCustomTutorName() : "나만의 AI 튜터";
        String selectedStyle = (request.personaName() != null) ? request.personaName() : plan.getPersona();
        String styleDescription = defineStyleTrait(selectedStyle, customName);
        String memoryContext = buildMemoryContext(plan.getId());

        if (request.dailyMood() != null) memoryContext += "\n- 오늘 학생 기분: " + request.dailyMood();

        String template = getPromptTemplate("CLASS_START");
        String promptText = String.format(template, customName, styleDescription, memoryContext, plan.getGoal(), request.dayCount());

        String aiMessage = chatClient.prompt().user(promptText).call().content();

        // [수정] 파일 저장 기반 TTS 생성 (URL 반환)
        String audioUrl = generateTieredTtsWithCache(aiMessage, user.getEffectiveTier());
        String imageUrl = generateImage("Illustration for " + plan.getGoal() + " class, " + selectedStyle + " style, vector art");

        return new TutorDTO.ClassStartResponse(request.dayCount() + "일차: " + plan.getGoal(), aiMessage, audioUrl, imageUrl, null, 25, 5);
    }

    // 2. 적응형 과제 생성 (TTS 없음)
    @Transactional(readOnly = true)
    public TutorDTO.DailyTestResponse generateTest(Long userId, Long planId, int dayCount) {
        StudyPlanEntity plan = studyMapper.findPlanById(planId);
        List<StudyLogEntity> logs = studyMapper.findLogsByPlanId(planId);
        double avgScore = logs.stream().mapToInt(StudyLogEntity::getTestScore).average().orElse(70.0);

        String difficulty = (avgScore >= 90) ? "HARD" : (avgScore <= 60 ? "EASY" : "NORMAL");
        String template = getPromptTemplate("TEST_ADAPTIVE");
        String promptText = String.format(template, plan.getGoal(), difficulty, (int) avgScore, dayCount);

        String question = chatClient.prompt().user(promptText).call().content();
        String imageUrl = generateImage("Visual aid for: " + question + ", subject: " + plan.getGoal());

        return new TutorDTO.DailyTestResponse("MISSION [" + difficulty + "]", question, imageUrl, null, 300);
    }

    // 3. Vision 첨삭 & 피드백
    @Transactional
    public TutorDTO.TestFeedbackResponse submitTest(Long userId, Long planId, String textAnswer, MultipartFile image) {
        UserEntity user = userMapper.findById(userId);
        StudyPlanEntity plan = studyMapper.findPlanById(planId);

        String aiResponse;
        if (image != null && !image.isEmpty()) {
            aiResponse = analyzeUniversalImage(image, plan.getGoal());
        } else {
            String template = getPromptTemplate("TEST_FEEDBACK");
            aiResponse = chatClient.prompt()
                    .user(String.format(template, plan.getGoal(), textAnswer))
                    .call().content();
        }

        int score = parseScore(aiResponse);
        String summary = aiResponse.contains("★") ? aiResponse.substring(aiResponse.indexOf("★")) : "요약 없음";
        int totalPointChange = ((score >= 80) ? 100 : (score <= 40 ? -50 : 0)) + evaluateAttitude(textAnswer);

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

        updateStreak(user);

        if (getCurrentDayCount(planId) == 1 && plan.getCustomTutorName() == null) {
            plan.setCustomTutorName("나만의 AI 튜터");
            studyMapper.updatePlan(plan);
        }

        // [수정] 파일 저장 기반 TTS 생성 (URL 반환)
        String audioUrl = generateTieredTtsWithCache(aiResponse.substring(0, Math.min(aiResponse.length(), 200)), user.getEffectiveTier());
        String expImage = (score < 100) ? generateImage("Explanation diagram for: " + plan.getGoal() + " - " + summary) : null;

        return new TutorDTO.TestFeedbackResponse(score, aiResponse, summary, audioUrl, expImage, null, isPassed);
    }

    // 4. 시험 문제 생성 (JSON 모드)
    @Transactional(readOnly = true)
    public TutorDTO.ExamGenerateResponse generateExam(Long userId, Long planId, int startDay, int endDay) {
        StudyPlanEntity plan = studyMapper.findPlanById(planId);
        List<StudyLogEntity> logs = studyMapper.findLogsBetweenDays(planId, startDay, endDay);

        if (logs.isEmpty()) throw new TutorooException("시험 범위에 해당하는 학습 기록이 없습니다.", ErrorCode.INVALID_INPUT_VALUE);

        String combinedSummary = logs.stream()
                .map(log -> log.getDayCount() + "일차: " + log.getDailySummary())
                .collect(Collectors.joining("\n"));

        String promptText = String.format("""
                분야: %s
                학습 자료: %s
                지시: 위 내용을 바탕으로 5개의 객관식 문제를 출제하세요.
                반드시 JSON 형식으로 반환하세요.
                """, plan.getGoal(), combinedSummary);

        return chatClient.prompt()
                .user(promptText)
                .call()
                .entity(TutorDTO.ExamGenerateResponse.class);
    }

    // 5. 중간/기말고사 제출
    @Transactional
    public TutorDTO.ExamResultResponse submitExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        String prompt = "학생 답안: " + request.answers() + ". 5문제 채점 및 해설.";
        String aiFeedback = chatClient.prompt().user(prompt).call().content();

        int score = parseScore(aiFeedback);
        int point = score * 5;
        userMapper.updateUserPointByPlan(request.planId(), point);
        return new TutorDTO.ExamResultResponse(score, (point > 200 ? 5 : 1), aiFeedback, new ArrayList<>(), score >= 60);
    }

    // 6. 커스텀 선생님 이름 변경
    @Transactional
    public void renameCustomTutor(Long planId, String newName) {
        StudyPlanEntity plan = studyMapper.findPlanById(planId);
        if (plan != null) {
            plan.setCustomTutorName(newName);
            studyMapper.updatePlan(plan);
        }
    }

    // --- Helper Methods ---

    private String analyzeUniversalImage(MultipartFile imageFile, String goal) {
        try {
            String promptText = String.format(getPromptTemplate("VISION_FEEDBACK"), goal);
            byte[] imageBytes = imageFile.getBytes();

            return chatClient.prompt()
                    .user(u -> u.text(promptText).media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageBytes)))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Vision Analysis Error: {}", e.getMessage());
            return "이미지 분석 중 오류가 발생했습니다.";
        }
    }

    private String defineStyleTrait(String selection, String customName) {
        if (selection.equals(customName)) return "오리지널 커스텀 스타일";
        return switch (selection) {
            case "호랑이" -> "호랑이 (엄격함, 스파르타, 강한 어조)";
            case "토끼" -> "토끼 (발랄함, 칭찬 위주, 빠른 속도)";
            case "거북이" -> "거북이 (매우 느림, 차분함, 반복 설명)";
            case "캥거루" -> "캥거루 (활동적, 친구 같은 친근함)";
            case "드래곤" -> "드래곤 (고어체, 지혜로운 조언)";
            default -> selection + " 스타일";
        };
    }

    private String buildMemoryContext(Long planId) {
        List<StudyLogEntity> recentLogs = studyMapper.findLogsByPlanId(planId);
        String context = recentLogs.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(5)
                .map(log -> String.format("- %d일차(%d점): %s", log.getDayCount(), log.getTestScore(), log.getStudentFeedback()))
                .collect(Collectors.joining("\n"));
        return context.isEmpty() ? "첫 만남입니다." : context;
    }

    @Transactional(readOnly = true)
    public String convertSpeechToText(MultipartFile audioFile) {
        try {
            return transcriptionModel.call(new AudioTranscriptionPrompt(new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() { return "speech.mp3"; }
            })).getResult().getOutput();
        } catch (Exception e) {
            throw new TutorooException("STT Error", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    protected void updateStreak(UserEntity user) {
        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastStudyDate();
        if (last == null) user.setCurrentStreak(1);
        else if (last.equals(today.minusDays(1))) user.setCurrentStreak(user.getCurrentStreak() + 1);
        else if (last.isBefore(today.minusDays(1))) user.setCurrentStreak(1);
        user.setLastStudyDate(today);
        userMapper.update(user);
    }

    @Transactional(readOnly = true)
    public TutorDTO.FeedbackChatResponse adjustCurriculum(Long userId, Long planId, String message) {
        UserEntity user = userMapper.findById(userId);
        String aiResponse = chatClient.prompt().user("피드백: " + message).call().content();
        return new TutorDTO.FeedbackChatResponse(aiResponse, generateTieredTtsWithCache(aiResponse, user.getEffectiveTier()));
    }

    @Transactional
    public void saveStudentFeedback(TutorDTO.TutorReviewRequest request) {
        studyMapper.updateStudentFeedback(request.planId(), request.dayCount(), request.feedback());
    }

    private int evaluateAttitude(String t) {
        try {
            String res = chatClient.prompt().user("답안: \"" + t + "\". 태도점수 0~10 숫자만.").call().content();
            return Integer.parseInt(res.replaceAll("[^0-9]", ""));
        } catch (Exception e) { return 0; }
    }

    private int getCurrentDayCount(Long id) {
        StudyLogEntity l = studyMapper.findLatestLogByPlanId(id);
        return l == null ? 1 : l.getDayCount();
    }

    private String getPromptTemplate(String k) {
        String c = commonMapper.findPromptContentByKey(k);
        return c != null ? c : "Prompt: " + k;
    }

    private String generateImage(String p) {
        try {
            return imageModel.call(new ImagePrompt(p, OpenAiImageOptions.builder().withModel("dall-e-3").withHeight(1024).withWidth(1024).build())).getResult().getOutput().getUrl();
        } catch (Exception e) { return null; }
    }

    // --- [핵심 수정: 데이터 처리(byte[])와 경로(Path) 분리] ---
    private String generateTieredTtsWithCache(String text, MembershipTier tier) {
        if (text == null || text.isEmpty()) return null;

        // 1. 해시 키 생성
        String voiceName = (tier.getTtsVoice() != null) ? tier.getTtsVoice() : "alloy";
        String hashKey = generateHash(text + ":" + voiceName);

        // 2. DB 캐시 확인 (있으면 파일 URL 반환)
        TtsCacheEntity cached = commonMapper.findTtsCacheByHash(hashKey);
        if (cached != null) {
            return cached.getAudioPath(); // DB에 저장된 URL 리턴
        }

        // 3. OpenAI 호출 -> **바이트 데이터(Data)** 획득
        byte[] audioBytes = callOpenAiTts(text, tier);
        if (audioBytes == null) return null;

        // 4. 파일 저장소에 저장 -> **접근 경로(Path/URL)** 획득
        // (FileStore.storeFile 메서드가 byte[]를 받아 저장 후 URL을 리턴한다고 가정)
        String audioUrl = fileStore.storeFile(audioBytes, ".mp3");

        // 5. DB에는 **경로(Path/URL)**만 저장
        commonMapper.saveTtsCache(TtsCacheEntity.builder()
                .textHash(hashKey)
                .audioPath(audioUrl)
                .build());

        return audioUrl;
    }

    // 반환 타입을 String(Base64)에서 byte[]로 변경하여 효율성 증대
    private byte[] callOpenAiTts(String text, MembershipTier tier) {
        try {
            OpenAiAudioApi.SpeechRequest.Voice voice = OpenAiAudioApi.SpeechRequest.Voice.ALLOY;
            try {
                if (tier.getTtsVoice() != null) voice = OpenAiAudioApi.SpeechRequest.Voice.valueOf(tier.getTtsVoice().toUpperCase());
            } catch (Exception e) {}

            SpeechResponse res = speechModel.call(
                    new SpeechPrompt(text, OpenAiAudioSpeechOptions.builder()
                            .model("tts-1")
                            .voice(voice)
                            .build())
            );
            // Base64 변환 없이 원본 바이트 배열 반환
            return res.getResult().getOutput();
        } catch (Exception e) {
            log.error("TTS Generation Error: {}", e.getMessage());
            return null;
        }
    }

    private String generateHash(String i) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(i.getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (byte b : h) s.append(String.format("%02x", b));
            return s.toString();
        } catch (Exception e) { return String.valueOf(i.hashCode()); }
    }

    private int parseScore(String t) {
        try {
            Matcher m = Pattern.compile("(점수|Score)\\s*:\\s*(\\d{1,3})").matcher(t);
            if (m.find()) return Integer.parseInt(m.group(2));
        } catch (Exception e) {}
        return 50;
    }
}