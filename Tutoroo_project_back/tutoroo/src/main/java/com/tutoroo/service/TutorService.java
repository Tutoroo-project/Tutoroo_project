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
import com.tutoroo.mapper.UserMapper;
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
    private final UserMapper userMapper;
    private final CommonMapper commonMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final FileStore fileStore;
    private final RedisTemplate<String, String> redisTemplate;

    // --- [1] 수업 시작 (수정됨: 튜터/커스텀 저장 + JSON 파싱 강화) ---
    @Transactional
    public TutorDTO.ClassStartResponse startClass(Long userId, TutorDTO.ClassStartRequest request) {
        StudyPlanEntity plan = studyMapper.findById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        // 1. 튜터 변경 감지 및 DB 저장 (기본값 Tiger 문제 해결)
        String requestedPersona = request.personaName().toUpperCase();
        String currentPersona = plan.getPersona() != null ? plan.getPersona().toUpperCase() : "";

        if (!requestedPersona.equals(currentPersona)) {
            log.info("🔄 튜터 변경 감지: {} -> {}", currentPersona, requestedPersona);
            plan.setPersona(requestedPersona);
            studyMapper.updatePlan(plan); // DB 업데이트
        }

        // 2. 기본 시스템 프롬프트 로드
        String basePersonaKey = "TEACHER_" + request.personaName();
        String baseSystemContent = commonMapper.findPromptContentByKey(basePersonaKey);
        if (baseSystemContent == null) baseSystemContent = "너는 열정적인 AI 과외 선생님이야.";

        // 3. 페르소나 및 커스텀 옵션 적용 (프롬프트 조립)
        String customName = plan.getCustomTutorName();
        String customReq = request.customOption(); // 프론트에서 받은 커스텀 요구사항

        StringBuilder promptBuilder = new StringBuilder();

        // (1) 기본 역할 부여
        promptBuilder.append(baseSystemContent);

        // (2) 커스텀 이름(본캐/부캐) 설정
        if (StringUtils.hasText(customName)) {
            promptBuilder.append(String.format("""
                    
                    [System Roleplay Instruction]
                    1. 너의 진짜 정체(본캐)는 '%s'라는 이름의 튜터야.
                    2. 하지만 오늘 수업에서는 위에서 설정된 기본 페르소나(부캐)로 연기해야 해.
                    3. 호칭은 '%s'라고 스스로를 소개해.
                    """, customName, customName));
        }

        // (3) [New] 사용자 커스텀 요구사항 반영
        if (StringUtils.hasText(customReq)) {
            promptBuilder.append(String.format("""
                    
                    [⭐️ 학생의 특별 요청 사항]
                    수업 진행 시 다음 요청을 반드시 반영해줘: "%s"
                    """, customReq));
        }

        String finalSystemPrompt = promptBuilder.toString();

        // 4. AI 오프닝 멘트 및 유동적 스케줄 요청
        String userPrompt = String.format("""
                상황: %d일차 수업 시작. 주제: %s. 학생 기분: %s.
                
                [지시사항]
                1. 오프닝 멘트를 작성하세요.
                2. 오늘 수업의 **세션별 시간(초 단위)**을 JSON 형식으로 제안하세요.
                   (필수 키: CLASS, BREAK, TEST, GRADING, EXPLANATION, AI_FEEDBACK, STUDENT_FEEDBACK)
                
                [매우 중요 - 응답 형식]
                반드시 아래 형식을 정확히 지키세요. JSON 데이터는 반드시 맨 마지막에 위치해야 합니다.
                주제 | 오프닝 멘트 | JSON_DATA
                
                예시:
                자바 기초 | 안녕하세요! 수업 시작합니다. | {"CLASS": 3000, "BREAK": 600}
                """, request.dayCount(), plan.getGoal(), request.dailyMood());

        String response = chatClientBuilder.build()
                .prompt(new Prompt(List.of(
                        new SystemMessage(finalSystemPrompt),
                        new UserMessage(userPrompt)
                )))
                .call()
                .content();

        // 5. 응답 파싱 로직 (JSON 분리 강화 - 채팅창 노출 방지)
        String topic = "오늘의 학습";
        String aiMessage = response;
        String scheduleJson = "{}";

        try {
            // 1단계: 맨 뒤에 있는 JSON 덩어리를 먼저 찾아서 잘라냄
            int jsonStartIndex = response.lastIndexOf("{");
            int jsonEndIndex = response.lastIndexOf("}");

            if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonStartIndex < jsonEndIndex) {
                // JSON 부분 추출
                scheduleJson = response.substring(jsonStartIndex, jsonEndIndex + 1);

                // 원본 텍스트에서 JSON 부분 제거
                String textPart = response.substring(0, jsonStartIndex).trim();

                // 2단계: 파이프(|)로 주제와 멘트 분리
                // 끝에 남은 파이프(|) 제거
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
                // JSON을 못 찾은 경우 (기존 방식 시도)
                String[] parts = response.split("\\|");
                if (parts.length > 0) topic = parts[0].trim();
                if (parts.length > 1) aiMessage = parts[1].trim();
            }
        } catch (Exception e) {
            log.error("Response Parsing Error", e);
        }

        // 6. JSON 파싱 (문자열 -> Map)
        Map<String, Integer> scheduleMap = new HashMap<>();
        try {
            scheduleMap = objectMapper.readValue(scheduleJson, Map.class);
        } catch (Exception e) {
            log.warn("⚠️ AI 스케줄 파싱 실패, 기본값 사용. JSON: {}", scheduleJson);
        }

        // 7. TTS 생성
        String audioUrl = generateTtsAudio(aiMessage, request.personaName());

        // 8. 리소스 매핑
        String imageUrl = "/images/tutors/" + request.personaName().toLowerCase() + ".png";
        String bgmUrl = "/audio/bgm/calm.mp3";

        return new TutorDTO.ClassStartResponse(
                topic, aiMessage, audioUrl, imageUrl, bgmUrl,
                10, 5, scheduleMap
        );
    }

    // --- [2] 데일리 테스트 생성 ---
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

    // --- [3] 테스트 제출 및 피드백 ---
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

    // --- [4] 커리큘럼 조정 채팅 ---
    @Transactional
    public TutorDTO.FeedbackChatResponse adjustCurriculum(Long userId, Long planId, String message) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String historyKey = "chat:history:" + planId;
        List<Message> messages = new ArrayList<>();

        String personaName = plan.getPersona() != null ? plan.getPersona() : "TIGER";
        String baseSystemContent = commonMapper.findPromptContentByKey("TEACHER_" + personaName);
        if (baseSystemContent == null) baseSystemContent = "친절한 AI 선생님입니다.";

        // 커스텀 이름 적용
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

        String audioUrl = generateTtsAudio(aiResponse, personaName);

        return new TutorDTO.FeedbackChatResponse(aiResponse, audioUrl);
    }

    // --- [5] 음성 인식 (STT) ---
    public String convertSpeechToText(MultipartFile audio) {
        try {
            File tempFile = File.createTempFile("stt_", ".mp3");
            audio.transferTo(tempFile);

            String text = transcriptionModel.call(new AudioTranscriptionPrompt(new FileSystemResource(tempFile))).getResult().getOutput();

            tempFile.delete();
            return text;
        } catch (Exception e) {
            log.error("STT Error: {}", e.getMessage());
            throw new TutorooException(ErrorCode.STT_PROCESSING_ERROR);
        }
    }

    // --- [6] 학생 피드백 저장 ---
    @Transactional
    public void saveStudentFeedback(TutorDTO.TutorReviewRequest request) {
        studyMapper.updateStudentFeedback(request.planId(), request.dayCount(), request.feedback());
    }

    // --- [7] 시험 생성 ---
    @Transactional(readOnly = true)
    public TutorDTO.ExamGenerateResponse generateExam(Long userId, Long planId, int startDay, int endDay) {
        List<TutorDTO.ExamGenerateResponse.ExamQuestion> questions = new ArrayList<>();
        questions.add(new TutorDTO.ExamGenerateResponse.ExamQuestion(1, "Java의 특징이 아닌 것은?", List.of("OOP", "Platform Independent", "Pointers", "Multi-threaded")));
        return new TutorDTO.ExamGenerateResponse("주간 평가", questions);
    }

    // --- [8] 시험 제출 ---
    @Transactional
    public TutorDTO.ExamResultResponse submitExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        return new TutorDTO.ExamResultResponse(
                90, 1, "훌륭해요! 만점에 가까운 점수입니다.", List.of(), true
        );
    }

    // --- [9] 커스텀 튜터 이름 변경 ---
    @Transactional
    public void renameCustomTutor(Long planId, String newName) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan != null) {
            plan.setCustomTutorName(newName);
            studyMapper.updatePlan(plan);
        }
    }

    // --- [Private] TTS 생성 및 파일 저장 ---
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