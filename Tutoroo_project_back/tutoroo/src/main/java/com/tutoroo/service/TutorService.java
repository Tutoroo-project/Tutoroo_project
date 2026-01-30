package com.tutoroo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.TutorDTO;
import com.tutoroo.entity.*;
import com.tutoroo.event.StudyCompletedEvent;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.CommonMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorService {

    private final StudyMapper studyMapper;
    private final CommonMapper commonMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final FileStore fileStore;
    private final RedisTemplate<String, String> redisTemplate;

    // --- [1] 수업 시작 (Class Start) ---
    @Transactional
    public TutorDTO.ClassStartResponse startClass(Long userId, TutorDTO.ClassStartRequest request) {
        StudyPlanEntity plan = studyMapper.findById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        // 1. 튜터 변경 감지 및 DB 저장
        String requestedPersona = request.personaName().toUpperCase();
        String currentPersona = plan.getPersona() != null ? plan.getPersona().toUpperCase() : "";

        if (!requestedPersona.equals(currentPersona)) {
            log.info("🔄 튜터 변경 감지: {} -> {}", currentPersona, requestedPersona);
            plan.setPersona(requestedPersona);
            studyMapper.updatePlan(plan);
        }

        // 2. 기본 시스템 프롬프트 로드
        String basePersonaKey = "TEACHER_" + request.personaName();
        String baseSystemContent = commonMapper.findPromptContentByKey(basePersonaKey);
        if (baseSystemContent == null) baseSystemContent = "너는 열정적인 AI 과외 선생님이야.";

        // 3. 페르소나 및 커스텀 옵션 적용
        String customName = plan.getCustomTutorName();
        String customReq = request.customOption();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(baseSystemContent);

        if (StringUtils.hasText(customName)) {
            promptBuilder.append(String.format("""
                    
                    [System Roleplay Instruction]
                    1. 너의 진짜 정체(본캐)는 '%s'라는 이름의 튜터야.
                    2. 하지만 오늘 수업에서는 위에서 설정된 기본 페르소나(부캐)로 연기해야 해.
                    3. 호칭은 '%s'라고 스스로를 소개해.
                    """, customName, customName));
        }

        if (StringUtils.hasText(customReq)) {
            promptBuilder.append(String.format("""
                    
                    [⭐️ 학생의 특별 요청 사항]
                    수업 진행 시 다음 요청을 반드시 반영해줘: "%s"
                    """, customReq));
        }

        String finalSystemPrompt = promptBuilder.toString();

        // 4. AI 오프닝 멘트 및 스케줄 요청 (프롬프트 수정됨)
        // [핵심 수정] CLASS 시간을 3000초(50분)로 고정하도록 강력하게 지시
        String userPrompt = String.format("""
                상황: %d일차 수업 시작. 주제: %s. 학생 기분: %s.
                
                [지시사항]
                1. 오프닝 멘트 후, **즉시 오늘의 핵심 개념을 설명하거나 흥미로운 질문을 던져서 수업을 시작해.**
                2. 학생이 바로 대답하거나 생각할 거리를 줘야 해.
                3. 오늘 수업의 **세션별 시간(초 단위)**을 JSON 형식으로 제안해.
                
                [★ 시간 설정 규칙 (절대 준수)]
                - "CLASS" (수업 시간): 반드시 **3000** (50분)으로 설정할 것.
                - "BREAK" (쉬는 시간): **600** (10분)으로 설정할 것.
                - 나머지(TEST 등)는 자유롭게 제안.
                
                [매우 중요 - 응답 형식]
                반드시 아래 형식을 정확히 지키세요. JSON 데이터는 반드시 맨 마지막에 위치해야 합니다.
                주제 | 수업 시작 멘트(질문 포함) | JSON_DATA
                
                예시:
                자바 기초 | 자바의 꽃은 객체지향이죠! 그럼 객체란 무엇일까요? | {"CLASS": 3000, "BREAK": 600, "TEST": 900}
                """, request.dayCount(), plan.getGoal(), request.dailyMood());

        String response = chatClientBuilder.build()
                .prompt(new Prompt(List.of(
                        new SystemMessage(finalSystemPrompt),
                        new UserMessage(userPrompt)
                )))
                .call()
                .content();

        // 5. 응답 파싱 로직
        String topic = "오늘의 학습";
        String aiMessage = response;
        String scheduleJson = "{}";

        try {
            int jsonStartIndex = response.lastIndexOf("{");
            int jsonEndIndex = response.lastIndexOf("}");

            if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonStartIndex < jsonEndIndex) {
                scheduleJson = response.substring(jsonStartIndex, jsonEndIndex + 1);
                String textPart = response.substring(0, jsonStartIndex).trim();

                if (textPart.endsWith("|")) {
                    textPart = textPart.substring(0, textPart.length() - 1).trim();
                }

                String[] parts = textPart.split("\\|");
                if (parts.length >= 2) {
                    topic = parts[0].trim();
                    aiMessage = parts[1].trim();
                } else {
                    aiMessage = textPart;
                }
            } else {
                String[] parts = response.split("\\|");
                if (parts.length > 0) topic = parts[0].trim();
                if (parts.length > 1) aiMessage = parts[1].trim();
            }
        } catch (Exception e) {
            log.error("Response Parsing Error", e);
        }

        // 6. JSON 파싱
        Map<String, Integer> scheduleMap = new HashMap<>();
        try {
            scheduleMap = objectMapper.readValue(scheduleJson, Map.class);
            // [안전장치] 혹시 AI가 말을 안 들었을 경우를 대비해 강제 덮어쓰기
            scheduleMap.put("CLASS", 3000);
        } catch (Exception e) {
            log.warn("⚠️ AI 스케줄 파싱 실패, 기본값 사용. JSON: {}", scheduleJson);
            scheduleMap.put("CLASS", 3000); // 파싱 실패 시에도 기본값 보장
        }

        // 7. TTS 생성 (needsTts가 true일 때만)
        String audioUrl = null;
        if (request.needsTts()) {
            audioUrl = generateTtsAudio(aiMessage, request.personaName());
        }

        // 8. 이미지 설정 (수업 시작은 튜터 이미지)
        String tutorImageUrl = "/images/tutors/" + request.personaName().toLowerCase() + ".png";
        String bgmUrl = "/audio/bgm/calm.mp3";

        return new TutorDTO.ClassStartResponse(
                topic, aiMessage, audioUrl, tutorImageUrl, bgmUrl,
                10, 5, scheduleMap
        );
    }

    // --- [2] 세션(모드) 변경 시 AI 멘트 및 이미지 생성 ---
    @Transactional
    public TutorDTO.SessionStartResponse startSession(Long userId, TutorDTO.SessionStartRequest request) {
        String mode = request.sessionMode();
        String personaName = request.personaName();

        // 1. 모드별 상황 및 이미지 설정
        String situationPrompt;
        String imageUrl = null;

        switch (mode) {
            case "BREAK" -> {
                situationPrompt = "상황: 수업이 끝나고 쉬는 시간(Break Time)이 시작되었어. 학생에게 '수고했어, 잠시 머리 좀 식히고 와'라는 뉘앙스로 격려해줘.";
                imageUrl = "/images/break_time.png";
            }
            case "TEST" -> {
                situationPrompt = "상황: 이제 데일리 테스트(Test) 시간이야. '오늘 배운 내용을 확인해볼까? 준비되면 시작하자'라고 긴장감을 줘.";
                imageUrl = "/images/quiz_bg.png";
            }
            case "GRADING" -> {
                situationPrompt = "상황: 학생이 테스트를 제출했고, AI인 네가 채점(Grading)을 진행하는 중이야. '잠시만 기다려, 꼼꼼히 확인해볼게'라고 말해줘.";
            }
            case "EXPLANATION" -> {
                situationPrompt = "상황: 채점이 끝났고 해설 강의(Explanation)를 시작할 차례야. '자, 틀린 문제랑 중요한 내용 다시 한번 짚어줄게'라고 리드해줘.";
            }
            case "AI_FEEDBACK" -> {
                situationPrompt = "상황: 오늘의 모든 학습이 끝나고 피드백(Feedback) 시간이야. 오늘 하루 고생했다고 마무리 인사를 해줘.";
            }
            case "STUDENT_FEEDBACK" -> {
                situationPrompt = "상황: 학생이 수업에 대해 평가하는 시간이야. '오늘 수업 어땠어? 솔직하게 말해줘'라고 물어봐.";
            }
            default -> situationPrompt = "상황: 다음 학습 단계로 넘어갔어. 자연스럽게 다음 진행을 유도해줘.";
        }

        // 2. 페르소나 적용
        String basePersonaKey = "TEACHER_" + personaName;
        String baseSystemContent = commonMapper.findPromptContentByKey(basePersonaKey);
        if (baseSystemContent == null) baseSystemContent = "너는 친절한 AI 선생님이야.";

        String systemPrompt = baseSystemContent + "\n\n[Instruction]\n" + situationPrompt + "\n한 두 문장으로 짧고 자연스럽게 말해.";

        // 3. AI 응답 생성
        String aiMessage = chatClientBuilder.build()
                .prompt(new Prompt(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage("현재 모드: " + mode + ". 멘트 시작해줘.")
                )))
                .call()
                .content();

        // 4. TTS 생성
        String audioUrl = null;
        if (request.needsTts()) {
            audioUrl = generateTtsAudio(aiMessage, personaName);
        }

        // 5. 이미지
        if (imageUrl == null) {
            imageUrl = "/images/tutors/" + personaName.toLowerCase() + ".png";
        }

        return new TutorDTO.SessionStartResponse(aiMessage, audioUrl, imageUrl);
    }

    // --- [3] 데일리 테스트 생성 ---
    @Transactional(readOnly = true)
    public TutorDTO.DailyTestResponse generateTest(Long userId, Long planId, int dayCount) {
        String question = "Java의 Garbage Collection이 주로 발생하는 메모리 영역은?";
        String voiceUrl = generateTtsAudio(question, "TIGER");

        return new TutorDTO.DailyTestResponse(
                "QUIZ",
                question,
                "/images/quiz_bg.png",
                voiceUrl,
                30
        );
    }

    // --- [4] 테스트 제출 및 피드백 ---
    @Transactional
    public TutorDTO.TestFeedbackResponse submitTest(Long userId, Long planId, String textAnswer, MultipartFile image) {
        StudyPlanEntity plan = studyMapper.findById(planId);

        String feedbackPrompt = String.format(
                "문제: Java GC 영역. 답안: %s. 채점하고 피드백해줘. 형식: 점수:XX | 피드백(한 문장)",
                textAnswer
        );

        String aiResponse = chatClientBuilder.build().prompt().user(feedbackPrompt).call().content();

        int score = parseScore(aiResponse);
        String feedbackMsg = aiResponse.contains("|") ?
                aiResponse.split("\\|")[1].trim() : aiResponse;
        boolean isPassed = score >= 60;

        StudyLogEntity logEntity = StudyLogEntity.builder()
                .planId(planId)
                .dayCount(1)
                .testScore(score)
                .aiFeedback(feedbackMsg)
                .isCompleted(isPassed)
                .pointChange(isPassed ? 100 : 10)
                .build();
        studyMapper.saveLog(logEntity);

        if (isPassed) {
            eventPublisher.publishEvent(new StudyCompletedEvent(userId, score));
        }

        String audioUrl = generateTtsAudio(feedbackMsg, plan.getPersona());

        return new TutorDTO.TestFeedbackResponse(
                score,
                feedbackMsg,
                "오늘의 학습 요약",
                audioUrl,
                "/images/feedback_good.png",
                "내일도 화이팅!",
                isPassed
        );
    }

    // --- [5] 커리큘럼 조정 채팅 ---
    @Transactional
    public TutorDTO.FeedbackChatResponse adjustCurriculum(Long userId, Long planId, String message, boolean needsTts) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String historyKey = "chat:history:" + planId;
        List<Message> messages = new ArrayList<>();

        String personaName = plan.getPersona() != null ? plan.getPersona() : "TIGER";
        String baseSystemContent = commonMapper.findPromptContentByKey("TEACHER_" + personaName);
        if (baseSystemContent == null) baseSystemContent = "친절한 AI 선생님입니다.";

        String customName = plan.getCustomTutorName();
        StringBuilder systemPrompt = new StringBuilder(baseSystemContent);

        if (StringUtils.hasText(customName)) {
            systemPrompt.append(String.format("""
                
                [Identity Override]
                Name: %s
                Style: %s
                Instruction: You are %s but acting in the style of %s. 
                Keep the conversation flowing naturally based on previous context.
                """, customName, personaName, customName, personaName));
        }

        messages.add(new SystemMessage(systemPrompt.toString()));

        try {
            List<String> historyJson = redisTemplate.opsForList().range(historyKey, 0, -1);
            if (historyJson != null) {
                for (String json : historyJson) {
                    Map<String, String> msgMap = objectMapper.readValue(json, Map.class);
                    String role = msgMap.get("role");
                    String content = msgMap.get("content");
                    if ("user".equals(role)) messages.add(new UserMessage(content));
                    else if ("assistant".equals(role)) messages.add(new AssistantMessage(content));
                }
            }
        } catch (Exception e) {
            log.error("History Load Error", e);
        }

        messages.add(new UserMessage(message));
        Prompt prompt = new Prompt(messages);
        String aiResponse = chatClientBuilder.build().prompt(prompt).call().content();

        try {
            String userJson = objectMapper.writeValueAsString(Map.of("role", "user", "content", message));
            String aiJson = objectMapper.writeValueAsString(Map.of("role", "assistant", "content", aiResponse));
            redisTemplate.opsForList().rightPush(historyKey, userJson);
            redisTemplate.opsForList().rightPush(historyKey, aiJson);

            if (redisTemplate.opsForList().size(historyKey) > 20) {
                redisTemplate.opsForList().trim(historyKey, -20, -1);
            }
            redisTemplate.expire(historyKey, 24, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("History Save Error", e);
        }

        String audioUrl = null;
        if (needsTts) {
            audioUrl = generateTtsAudio(aiResponse, personaName);
        }

        return new TutorDTO.FeedbackChatResponse(aiResponse, audioUrl);
    }

    // --- [6] STT ---
    public String convertSpeechToText(MultipartFile audio) {
        try {
            String originalFilename = audio.getOriginalFilename();
            String extension = ".webm";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            File tempFile = File.createTempFile("stt_", extension);
            audio.transferTo(tempFile);

            String text = transcriptionModel.call(new AudioTranscriptionPrompt(new FileSystemResource(tempFile))).getResult().getOutput();

            tempFile.delete();
            return text;
        } catch (Exception e) {
            log.error("STT Error: {}", e.getMessage());
            throw new TutorooException(ErrorCode.STT_PROCESSING_ERROR);
        }
    }

    // --- [기타 메서드들] ---
    @Transactional
    public void saveStudentFeedback(TutorDTO.TutorReviewRequest request) {
        studyMapper.updateStudentFeedback(request.planId(), request.dayCount(), request.feedback());
    }

    @Transactional(readOnly = true)
    public TutorDTO.ExamGenerateResponse generateExam(Long userId, Long planId, int startDay, int endDay) {
        List<TutorDTO.ExamGenerateResponse.ExamQuestion> questions = new ArrayList<>();
        questions.add(new TutorDTO.ExamGenerateResponse.ExamQuestion(1, "Java의 특징이 아닌 것은?", List.of("OOP", "Platform Independent", "Pointers", "Multi-threaded")));
        return new TutorDTO.ExamGenerateResponse("주간 평가", questions);
    }

    @Transactional
    public TutorDTO.ExamResultResponse submitExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        return new TutorDTO.ExamResultResponse(90, 1, "훌륭해요!", List.of(), true);
    }

    @Transactional
    public void renameCustomTutor(Long planId, String newName) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan != null) {
            plan.setCustomTutorName(newName);
            studyMapper.updatePlan(plan);
        }
    }

    // --- [TTS 생성 (Private)] ---
    private String generateTtsAudio(String text, String personaName) {
        try {
            String textHash = generateHash(text + (personaName != null ? personaName : "DEFAULT"));
            TtsCacheEntity cached = commonMapper.findTtsCacheByHash(textHash);
            if (cached != null) return cached.getAudioPath();

            OpenAiAudioApi.SpeechRequest.Voice voice = OpenAiAudioApi.SpeechRequest.Voice.ALLOY;
            if (personaName != null) {
                String pUpper = personaName.toUpperCase();
                if (pUpper.contains("TIGER") || pUpper.contains("호랑이")) voice = OpenAiAudioApi.SpeechRequest.Voice.ONYX;
                else if (pUpper.contains("RABBIT") || pUpper.contains("토끼")) voice = OpenAiAudioApi.SpeechRequest.Voice.NOVA;
                else if (pUpper.contains("KANGAROO") || pUpper.contains("캥거루")) voice = OpenAiAudioApi.SpeechRequest.Voice.SHIMMER;
                else if (pUpper.contains("DRAGON") || pUpper.contains("용")) voice = OpenAiAudioApi.SpeechRequest.Voice.ECHO;
                else if (pUpper.contains("TURTLE") || pUpper.contains("거북이")) voice = OpenAiAudioApi.SpeechRequest.Voice.ALLOY;
            }

            SpeechResponse res = speechModel.call(
                    new SpeechPrompt(text, OpenAiAudioSpeechOptions.builder()
                            .model("tts-1")
                            .voice(voice)
                            .build())
            );
            byte[] audioData = res.getResult().getOutput();

            String fileUrl = fileStore.storeFile(audioData, ".mp3");
            commonMapper.saveTtsCache(TtsCacheEntity.builder().textHash(textHash).audioPath(fileUrl).build());

            return fileUrl;
        } catch (Exception e) {
            log.error("TTS Fail", e);
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