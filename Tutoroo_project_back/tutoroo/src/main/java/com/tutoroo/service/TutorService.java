package com.tutoroo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.TutorDTO;
import com.tutoroo.entity.*;
import com.tutoroo.event.StudyCompletedEvent;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.CommonMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.util.FileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private final StudyMapper studyMapper;
    private final UserMapper userMapper;
    private final CommonMapper commonMapper;
    private final FileStore fileStore;
    private final ApplicationEventPublisher eventPublisher;
    private final PetService petService; // [추가] 펫 버프 시스템 연동

    public TutorService(ChatClient.Builder chatClientBuilder,
                        OpenAiAudioSpeechModel speechModel,
                        OpenAiImageModel imageModel,
                        OpenAiAudioTranscriptionModel transcriptionModel,
                        RedisTemplate<String, String> redisTemplate,
                        ObjectMapper objectMapper,
                        StudyMapper studyMapper,
                        UserMapper userMapper,
                        CommonMapper commonMapper,
                        FileStore fileStore,
                        ApplicationEventPublisher eventPublisher,
                        PetService petService) { // [추가] 생성자 주입
        this.chatClient = chatClientBuilder
                .defaultSystem("당신은 Tutoroo의 AI 튜터입니다. 학생에게 친절하고 명확하게 설명해주세요.")
                .build();
        this.speechModel = speechModel;
        this.imageModel = imageModel;
        this.transcriptionModel = transcriptionModel;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.studyMapper = studyMapper;
        this.userMapper = userMapper;
        this.commonMapper = commonMapper;
        this.fileStore = fileStore;
        this.eventPublisher = eventPublisher;
        this.petService = petService;
    }

    // --- [핵심 기능 1: 수업 시작] ---
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

        String audioUrl = generateTieredTtsWithCache(aiMessage, user.getEffectiveTier());
        String imageUrl = generateImage("Illustration for " + plan.getGoal() + " class, " + selectedStyle + " style, vector art");

        return new TutorDTO.ClassStartResponse(request.dayCount() + "일차: " + plan.getGoal(), aiMessage, audioUrl, imageUrl, null, 25, 5);
    }

    // --- [핵심 기능 2: 적응형 과제 생성] ---
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

    // --- [핵심 기능 3: 시험 제출 및 Vision 피드백] ---
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

        // 1. 기본 포인트 계산
        int basePointChange = ((score >= 80) ? 100 : (score <= 40 ? -50 : 0)) + evaluateAttitude(textAnswer);
        int finalPointChange = basePointChange;

        // 2. [RPG 요소 적용] 펫 버프 확인 (포인트를 얻는 경우에만 적용)
        if (basePointChange > 0) {
            double multiplier = petService.getPointMultiplier(userId);
            if (multiplier > 1.0) {
                finalPointChange = (int) (basePointChange * multiplier);
                log.info("펫 버프 발동! User: {}, Multiplier: {}, Point: {} -> {}", userId, multiplier, basePointChange, finalPointChange);
            }
        }

        if (finalPointChange != 0) userMapper.updateUserPointByPlan(planId, finalPointChange);

        boolean isPassed = score >= 60;
        int currentDay = getCurrentDayCount(planId);

        studyMapper.saveLog(StudyLogEntity.builder()
                .planId(planId)
                .dayCount(currentDay)
                .testScore(score)
                .aiFeedback(aiResponse)
                .dailySummary(summary)
                .pointChange(finalPointChange)
                .isCompleted(isPassed)
                .build());

        updateStreak(user);

        // 펫 성장 연동: 합격 시 이벤트 발행
        if (isPassed) {
            try {
                eventPublisher.publishEvent(new StudyCompletedEvent(userId, score));
            } catch (Exception e) {
                log.warn("펫 경험치 지급 실패 (이벤트 오류): {}", e.getMessage());
            }
        }

        if (currentDay == 1 && plan.getCustomTutorName() == null) {
            plan.setCustomTutorName("나만의 AI 튜터");
            studyMapper.updatePlan(plan);
        }

        String audioUrl = generateTieredTtsWithCache(aiResponse.substring(0, Math.min(aiResponse.length(), 200)), user.getEffectiveTier());
        String expImage = (score < 100) ? generateImage("Explanation diagram for: " + plan.getGoal() + " - " + summary) : null;

        return new TutorDTO.TestFeedbackResponse(score, aiResponse, summary, audioUrl, expImage, null, isPassed);
    }

    // --- [핵심 기능 4: 대화형 피드백 (Redis Chat Memory 적용)] ---
    @Transactional(readOnly = true)
    public TutorDTO.FeedbackChatResponse adjustCurriculum(Long userId, Long planId, String message) {
        UserEntity user = userMapper.findById(userId);
        String conversationKey = "chat:" + planId + ":" + userId;

        List<Message> history = loadChatHistory(conversationKey);

        String aiResponse = chatClient.prompt()
                .messages(history)
                .user(message)
                .call()
                .content();

        saveChatMessage(conversationKey, new UserMessage(message));
        saveChatMessage(conversationKey, new AssistantMessage(aiResponse));

        String audioUrl = generateTieredTtsWithCache(aiResponse, user.getEffectiveTier());

        return new TutorDTO.FeedbackChatResponse(aiResponse, audioUrl);
    }

    // --- [핵심 기능 5: 시험 문제 생성 (JSON)] ---
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

    // --- [핵심 기능 6: 시험 결과 제출] ---
    @Transactional
    public TutorDTO.ExamResultResponse submitExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        String prompt = "학생 답안: " + request.answers() + ". 5문제 채점 및 해설.";
        String aiFeedback = chatClient.prompt().user(prompt).call().content();

        int score = parseScore(aiFeedback);

        // 1. 기본 포인트
        int basePoint = score * 5;
        int finalPoint = basePoint;

        // 2. [RPG 요소 적용] 펫 버프 확인
        if (basePoint > 0) {
            double multiplier = petService.getPointMultiplier(userId);
            if (multiplier > 1.0) {
                finalPoint = (int) (basePoint * multiplier);
            }
        }

        userMapper.updateUserPointByPlan(request.planId(), finalPoint);

        // 펫 성장 연동 (시험도 60점 넘으면 경험치)
        if (score >= 60) {
            try {
                eventPublisher.publishEvent(new StudyCompletedEvent(userId, score));
            } catch (Exception e) {
                log.warn("펫 경험치 지급 실패: {}", e.getMessage());
            }
        }

        return new TutorDTO.ExamResultResponse(score, (finalPoint > 200 ? 5 : 1), aiFeedback, new ArrayList<>(), score >= 60);
    }

    // --- [기타 기능] ---
    @Transactional
    public void renameCustomTutor(Long planId, String newName) {
        StudyPlanEntity plan = studyMapper.findPlanById(planId);
        if (plan != null) {
            plan.setCustomTutorName(newName);
            studyMapper.updatePlan(plan);
        }
    }

    @Transactional(readOnly = true)
    public String convertSpeechToText(MultipartFile audioFile) {
        try {
            return transcriptionModel.call(new AudioTranscriptionPrompt(new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() { return "speech.mp3"; }
            })).getResult().getOutput();
        } catch (Exception e) {
            log.error("STT Error: {}", e.getMessage());
            throw new TutorooException("STT Error", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void saveStudentFeedback(TutorDTO.TutorReviewRequest request) {
        studyMapper.updateStudentFeedback(request.planId(), request.dayCount(), request.feedback());
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

    // --- [Private Helpers] ---

    private int getCurrentDayCount(Long planId) {
        List<StudyLogEntity> logs = studyMapper.findLogsByPlanId(planId);
        if (logs == null || logs.isEmpty()) return 1;
        return logs.stream().mapToInt(StudyLogEntity::getDayCount).max().orElse(0) + 1;
    }

    private List<Message> loadChatHistory(String key) {
        List<Message> messages = new ArrayList<>();
        try {
            List<String> jsonHistory = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonHistory != null) {
                for (String json : jsonHistory) {
                    ChatMessageDto dto = objectMapper.readValue(json, ChatMessageDto.class);
                    if ("USER".equals(dto.role)) messages.add(new UserMessage(dto.content));
                    else messages.add(new AssistantMessage(dto.content));
                }
            }
        } catch (Exception e) {
            log.error("채팅 기록 로드 실패", e);
        }
        return messages;
    }

    private void saveChatMessage(String key, Message message) {
        try {
            String role;
            String content = message.getText(); // M6+ 대응

            if (message instanceof UserMessage) {
                role = "USER";
            } else if (message instanceof AssistantMessage) {
                role = "ASSISTANT";
            } else {
                return;
            }

            String json = objectMapper.writeValueAsString(new ChatMessageDto(role, content));

            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("채팅 기록 저장 실패", e);
        }
    }

    private record ChatMessageDto(String role, String content) {}

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
            return "이미지 분석 중 오류가 발생했습니다: " + e.getMessage();
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

    private String getPromptTemplate(String k) {
        String c = commonMapper.findPromptContentByKey(k);
        return c != null ? c : "Prompt: " + k;
    }

    private String generateImage(String p) {
        try {
            return imageModel.call(new ImagePrompt(p, OpenAiImageOptions.builder().withModel("dall-e-3").withHeight(1024).withWidth(1024).build())).getResult().getOutput().getUrl();
        } catch (Exception e) { return null; }
    }

    private String generateTieredTtsWithCache(String text, MembershipTier tier) {
        if (text == null || text.isEmpty()) return null;

        String voiceName = (tier.getTtsVoice() != null) ? tier.getTtsVoice() : "alloy";
        String hashKey = generateHash(text + ":" + voiceName);

        TtsCacheEntity cached = commonMapper.findTtsCacheByHash(hashKey);
        if (cached != null) {
            return cached.getAudioPath();
        }

        byte[] audioBytes = callOpenAiTts(text, tier);
        if (audioBytes == null) return null;

        String audioUrl = fileStore.storeFile(audioBytes, ".mp3");

        commonMapper.saveTtsCache(TtsCacheEntity.builder()
                .textHash(hashKey)
                .audioPath(audioUrl)
                .build());

        return audioUrl;
    }

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

    private int evaluateAttitude(String t) {
        try {
            String res = chatClient.prompt().user("답안: \"" + t + "\". 태도점수 0~10 숫자만.").call().content();
            return Integer.parseInt(res.replaceAll("[^0-9]", ""));
        } catch (Exception e) { return 0; }
    }
}