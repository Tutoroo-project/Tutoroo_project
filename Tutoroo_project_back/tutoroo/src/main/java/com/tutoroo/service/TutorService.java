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
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorService {

    private final StudyMapper studyMapper;
    private final CommonMapper commonMapper;
    private final OpenAiChatModel chatModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final FileStore fileStore;
    private final RedisTemplate<String, String> redisTemplate;

    // =================================================================================
    // 1. 수업 진행 (로드맵 절대 준수 스케줄링)
    // =================================================================================

    @Transactional
    public TutorDTO.ClassStartResponse startClass(Long userId, TutorDTO.ClassStartRequest request) {
        StudyPlanEntity plan = studyMapper.findById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        updatePersonaIfChanged(plan, request.personaName());

        // [프롬프트: 학습 총량 보존의 법칙]
        String userPrompt = String.format("""
                [학습 컨텍스트]
                - 과목: %s
                - 오늘의 미션: %d일차 커리큘럼 완주 (타협 불가)
                - 학생 기분: %s
                - 학생 요청: "%s"
                
                [지시사항: 페이스메이커 역할 수행]
                1. **목표 고수**: 학생이 힘들다고 해도 "오늘은 조금만 하자"는 안 돼. 대신 "쉬는 시간을 쪼개서라도 끝내자"라고 독려해.
                2. **전략적 스케줄링**:
                   - 기분이 좋음: 50분 풀타임 집중 (CLASS: 3000, BREAK: 600)
                   - 기분이 나쁨/지침: 뽀모도로 기법 적용 (CLASS: 1500, BREAK: 300 반복 제안)
                   - **어떤 경우에도 총 학습량(CLASS 합계)은 줄이지 마.**
                3. **오프닝 멘트**: 학생의 감정에 공감하되, 오늘 학습의 중요성을 강조하며 비장하게 시작해.
                
                [응답 형식]
                주제 | 멘트 | {"CLASS": 3000, "BREAK": 600}
                """,
                plan.getGoal(),
                request.dayCount(),
                request.dailyMood(),
                request.customOption() != null ? request.customOption() : "없음"
        );

        String systemPrompt = buildBaseSystemPrompt(plan, request.customOption()) +
                "\n너는 학생의 목표 달성을 최우선으로 여기는 최고의 코치야. 감정에 휘둘려 목표를 포기하게 두지 마.";

        String response = chatModel.call(new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ))).getResult().getOutput().getText();

        ParsedResponse parsed = parseScheduleResponse(response);
        String audioUrl = request.needsTts() ? generateTtsAudio(parsed.aiMessage, plan.getPersona()) : null;
        String tutorImageUrl = "/images/tutors/" + plan.getPersona().toLowerCase() + ".png";

        return new TutorDTO.ClassStartResponse(
                parsed.topic, parsed.aiMessage, audioUrl, tutorImageUrl, "/audio/bgm/calm.mp3",
                10, 5, parsed.schedule
        );
    }

    @Transactional
    public TutorDTO.SessionStartResponse startSession(Long userId, TutorDTO.SessionStartRequest request) {
        String mode = request.sessionMode();
        String personaName = request.personaName();

        // [프롬프트: 상황별 동기부여]
        String situation = switch (mode) {
            case "BREAK" -> "상황: 휴식. '뇌가 쉴 시간을 줘야 다음 지식이 들어와. 잠깐 눈 좀 붙여.'라고 과학적으로 조언해.";
            case "TEST" -> "상황: 테스트. '지금까지 배운 건 네 것이 되었을까? 확인해보자.'라며 도전 의식을 자극해.";
            case "GRADING" -> "상황: 채점. '결과보다 과정이 중요해. 꼼꼼히 봐줄게.'라고 안심시켜.";
            case "AI_FEEDBACK" -> "상황: 종료. 오늘 배운 핵심을 한 줄 요약해주고, 성취감을 느끼게 해줘.";
            default -> "상황: 수업 중. 집중력이 흐트러지지 않게 주의를 환기해.";
        };

        String basePrompt = commonMapper.findPromptContentByKey("TEACHER_" + personaName);
        if (basePrompt == null) basePrompt = "너는 유능한 AI 튜터야.";

        String aiMessage = chatModel.call(new Prompt(List.of(
                new SystemMessage(basePrompt),
                new UserMessage(situation)
        ))).getResult().getOutput().getText();

        String audioUrl = request.needsTts() ? generateTtsAudio(aiMessage, personaName) : null;
        String imageUrl = "/images/tutors/" + personaName.toLowerCase() + ".png";

        return new TutorDTO.SessionStartResponse(aiMessage, audioUrl, imageUrl);
    }

    // =================================================================================
    // 2. 데일리 테스트 생성
    // =================================================================================
    @Transactional(readOnly = true)
    public TutorDTO.DailyTestResponse generateTest(Long userId, Long planId, int dayCount) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String prompt = String.format("""
                [데일리 테스트 생성]
                - 과목: %s
                - 진도: %d일차
                - 페르소나: %s
                
                오늘 학습한 내용을 확인하는 가벼운 4지선다 퀴즈 1개를 JSON으로 만들어줘.
                형식:
                {
                    "type": "QUIZ",
                    "question": "문제 내용 (재미있게)",
                    "imageUrl": null,
                    "options": ["보기1", "보기2", "보기3", "보기4"],
                    "answerIndex": 0
                }
                """, plan.getGoal(), dayCount, plan.getPersona());

        String response = chatModel.call(prompt);
        String cleaned = cleanJson(response);

        try {
            return objectMapper.readValue(cleaned, TutorDTO.DailyTestResponse.class);
        } catch (Exception e) {
            // 파싱 실패 시 기본 응답 반환 (안전장치)
            return new TutorDTO.DailyTestResponse(
                    "QUIZ", "오늘 배운 내용을 스스로 정리해보세요!", null,
                    List.of("네", "아니오", "글쎄요", "모르겠어요"), 0
            );
        }
    }

    // =================================================================================
    // 3. 범용 멀티모달 시험 (Universal Exam Engine)
    // =================================================================================

    @Transactional(readOnly = true)
    public TutorDTO.ExamGenerateResponse generateExam(Long userId, Long planId) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        StudyLogEntity lastLog = studyMapper.findLatestLogByPlanId(planId);
        String topic = (lastLog != null) ? lastLog.getContentSummary() : "기초 입문";

        String promptText = String.format("""
            [Role] You are a world-class expert tutor in '%s'.
            [Topic] %s
            
            [Mission: Universal Assessment]
            Analyze the subject nature and generate 3 specialized questions to verify the student's capability.
            
            1. **Analyze Domain**: Is this subject about Logic (Coding), Visuals (Art), Audio (Music), or Knowledge (History)?
            2. **Select Question Types**:
               - Use 'CODE_FILL_IN' or 'SHORT_ANSWER' for logic/implementation.
               - Use 'VISUAL_ANALYSIS' for subjects requiring observation (Art, Biology, Architecture).
               - Use 'MULTIPLE_CHOICE' for checking conceptual understanding.
            3. **Generate Questions**:
               - Q1: Conceptual Trap (Must be tricky).
               - Q2: Visual/Structural Analysis (Description based, no image url needed yet).
               - Q3: Practical Application (Real-world scenario).
            
            [JSON Output Format (Strict)]
            {
                "title": "%s 실전 역량 평가",
                "questions": [
                    {
                        "number": 1,
                        "type": "MULTIPLE_CHOICE", 
                        "question": "...",
                        "options": ["..."],
                        "referenceMediaUrl": null,
                        "codeTemplate": null
                    },
                    {
                        "number": 2,
                        "type": "VISUAL_ANALYSIS", 
                        "question": "Scenario description...",
                        "referenceMediaUrl": null,
                        "codeTemplate": null
                    }
                ]
            }
            """, plan.getGoal(), topic, plan.getGoal());

        String jsonResponse = chatModel.call(promptText);
        String cleanedJson = cleanJson(jsonResponse);

        try {
            return objectMapper.readValue(cleanedJson, TutorDTO.ExamGenerateResponse.class);
        } catch (JsonProcessingException e) {
            log.error("시험 생성 JSON 파싱 실패", e);
            return createFallbackExam(topic);
        }
    }

    // 주간/월간 시험 생성 (오버로딩)
    @Transactional(readOnly = true)
    public TutorDTO.ExamGenerateResponse generateExam(Long userId, Long planId, int startDay, int endDay) {
        return generateExam(userId, planId);
    }

    // =================================================================================
    // 4. 범용 채점 (Universal Grading)
    // =================================================================================

    @Transactional
    public TutorDTO.ExamResultResponse evaluateExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        StringBuilder summary = new StringBuilder();
        for (TutorDTO.ExamSubmitRequest.SubmittedAnswer ans : request.answers()) {
            summary.append(String.format("- Q%d: ", ans.number()));
            if (ans.textAnswer() != null) summary.append(ans.textAnswer());
            if (ans.selectedOptionIndex() != null) summary.append("선택: ").append(ans.selectedOptionIndex());
            if (ans.attachmentUrl() != null) summary.append(" [파일 제출: ").append(ans.attachmentUrl()).append("]");
            summary.append("\n");
        }

        String promptText = String.format("""
            학생 답안 채점 요청.
            이 과목의 **최고 전문가 관점**에서 채점해.
            
            [평가 기준]
            1. **통찰력**: 단순 정답 여부를 넘어, 학생이 원리를 이해하고 있는지 파악해.
            2. **창의성**: (예체능 계열일 경우) 독창적인 접근 방식을 높게 평가해.
            3. **효율성**: (공학 계열일 경우) 더 나은 해결책이 있다면 'Correction'에 제시해.
            
            [답안 데이터]
            %s
            
            [JSON 응답]
            {"totalScore": 0, "isPassed": false, "aiComment": "...", "feedbacks": []}
            """, summary.toString());

        String jsonResponse = chatModel.call(promptText);
        String cleanedJson = cleanJson(jsonResponse);

        try {
            TutorDTO.ExamResultResponse result = objectMapper.readValue(cleanedJson, TutorDTO.ExamResultResponse.class);
            if (result.isPassed()) {
                eventPublisher.publishEvent(new StudyCompletedEvent(userId, result.totalScore()));
            }
            return result;
        } catch (JsonProcessingException e) {
            log.error("채점 JSON 파싱 실패", e);
            // [Fix] ErrorCode.AI_SERVICE_ERROR -> ErrorCode.AI_PROCESSING_ERROR (변수명 일치시킴)
            throw new TutorooException("채점 중 오류가 발생했습니다.", ErrorCode.AI_PROCESSING_ERROR);
        }
    }

    // [New] 컨트롤러의 submitExam 호출과 매핑되는 메서드
    public TutorDTO.ExamResultResponse submitExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        return evaluateExam(userId, request);
    }

    // [Legacy] 데일리 테스트 (기존 유지)
    @Transactional
    public TutorDTO.TestFeedbackResponse submitTest(Long userId, Long planId, String textAnswer, MultipartFile image) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        String prompt = "문제: " + plan.getGoal() + ". 답안: " + textAnswer + ". 점수(0~100)와 피드백 줘.";
        String res = chatModel.call(prompt);
        int score = parseScore(res);

        studyMapper.saveLog(StudyLogEntity.builder()
                .planId(planId).dayCount(0).testScore(score).aiFeedback(res)
                .isCompleted(score >= 60).pointChange(score >= 60 ? 50 : 10).build());

        return new TutorDTO.TestFeedbackResponse(
                score, res, "요약", requestTts(res, plan.getPersona()),
                "/images/feedback.png", "화이팅", score >= 60
        );
    }

    // =================================================================================
    // 5. 상담 및 STT
    // =================================================================================

    @Transactional
    public TutorDTO.FeedbackChatResponse adjustCurriculum(Long userId, Long planId, String message, boolean needsTts) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String historyKey = "chat:history:" + planId;
        List<Message> messages = loadHistory(historyKey);

        String basePrompt = commonMapper.findPromptContentByKey("TEACHER_" + plan.getPersona());

        String counselingPrompt = basePrompt + """
            \n[모드: 학습 조율 상담]
            1. 학생의 감정은 충분히 공감해줘. ("힘들었겠구나")
            2. 하지만 **목표는 타협하지 마.** ("그래도 이 부분은 중요하니까 내가 더 쉽게 설명해줄게")
            3. 포기하려는 학생을 다시 책상에 앉히는 것이 너의 임무야.
            """;

        messages.add(0, new SystemMessage(counselingPrompt));
        messages.add(new UserMessage(message));

        String aiResponse = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();
        saveHistory(historyKey, message, aiResponse);

        String audioUrl = needsTts ? generateTtsAudio(aiResponse, plan.getPersona()) : null;
        return new TutorDTO.FeedbackChatResponse(aiResponse, audioUrl);
    }

    public String convertSpeechToText(MultipartFile audio) {
        try {
            File tempFile = File.createTempFile("stt_", ".webm");
            audio.transferTo(tempFile);
            String text = transcriptionModel.call(new AudioTranscriptionPrompt(new FileSystemResource(tempFile))).getResult().getOutput();
            tempFile.delete();
            return text;
        } catch (Exception e) {
            log.error("STT Error", e);
            throw new TutorooException(ErrorCode.STT_PROCESSING_ERROR);
        }
    }

    @Transactional
    public void saveStudentFeedback(TutorDTO.TutorReviewRequest request) {
        studyMapper.updateStudentFeedback(request.planId(), request.dayCount(), request.feedback());
    }

    // [New] 커스텀 튜터 이름 변경
    @Transactional
    public void renameCustomTutor(Long planId, String newName) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        plan.setCustomTutorName(newName);
        studyMapper.updatePlan(plan);
    }

    // =================================================================================
    // 6. Private Helpers
    // =================================================================================

    private void updatePersonaIfChanged(StudyPlanEntity plan, String newPersona) {
        if (!plan.getPersona().equalsIgnoreCase(newPersona)) {
            plan.setPersona(newPersona.toUpperCase());
            studyMapper.updatePlan(plan);
        }
    }

    private String buildBaseSystemPrompt(StudyPlanEntity plan, String customOption) {
        String base = commonMapper.findPromptContentByKey("TEACHER_" + plan.getPersona());
        if (base == null) base = "너는 열정적인 AI 선생님이야.";

        StringBuilder sb = new StringBuilder(base);
        if (StringUtils.hasText(plan.getCustomTutorName())) {
            sb.append("\n이름은 '").append(plan.getCustomTutorName()).append("'로 연기해.");
        }
        return sb.toString();
    }

    private ParsedResponse parseScheduleResponse(String response) {
        Map<String, Integer> schedule = new HashMap<>();
        String topic = "오늘의 학습", msg = response;
        try {
            int idx = response.lastIndexOf("{");
            if (idx != -1) {
                schedule = objectMapper.readValue(response.substring(idx), Map.class);
                if (schedule.getOrDefault("CLASS", 0) < 600) schedule.put("CLASS", 1800);
                msg = response.substring(0, idx).trim();
            } else {
                schedule.put("CLASS", 1800);
                schedule.put("BREAK", 300);
            }
        } catch (Exception e) {
            schedule.put("CLASS", 1800);
        }
        return new ParsedResponse(topic, msg, schedule);
    }

    private String generateTtsAudio(String text, String personaName) {
        try {
            String hash = generateHash(text + personaName);
            TtsCacheEntity cached = commonMapper.findTtsCacheByHash(hash);
            if (cached != null) return cached.getAudioPath();
            SpeechResponse res = speechModel.call(new SpeechPrompt(text, OpenAiAudioSpeechOptions.builder().model("tts-1").voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY).build()));
            String url = fileStore.storeFile(res.getResult().getOutput(), ".mp3");
            commonMapper.saveTtsCache(TtsCacheEntity.builder().textHash(hash).audioPath(url).build());
            return url;
        } catch (Exception e) { return null; }
    }

    private TutorDTO.ExamGenerateResponse createFallbackExam(String topic) {
        return new TutorDTO.ExamGenerateResponse(
                topic + " 기초 평가",
                List.of(new TutorDTO.ExamGenerateResponse.ExamQuestion(
                        1,
                        QuestionType.MULTIPLE_CHOICE,
                        topic + "의 개념은?",
                        null,
                        null,
                        List.of("A", "B", "C", "D"),
                        null
                ))
        );
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }

    private String generateHash(String input) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder s = new StringBuilder();
        for (byte b : h) s.append(String.format("%02x", b));
        return s.toString();
    }

    private int parseScore(String text) {
        Matcher m = Pattern.compile("(\\d{1,3})").matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 50;
    }

    private record ParsedResponse(String topic, String aiMessage, Map<String, Integer> schedule) {}
    private String requestTts(String text, String persona) { return generateTtsAudio(text, persona); }
    private List<Message> loadHistory(String key) { return new ArrayList<>(); }
    private void saveHistory(String key, String u, String a) {}
}