package com.tutoroo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.TutorDTO;
import com.tutoroo.entity.*;
import com.tutoroo.event.StudyCompletedEvent;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.ChatMapper;
import com.tutoroo.mapper.CommonMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.messages.AssistantMessage;
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
    private final ChatMapper chatMapper; // DB 기반 영구 기억 장치
    private final OpenAiChatModel chatModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final FileStore fileStore;
    private final RedisTemplate<String, String> redisTemplate;

    // =================================================================================
    // 1. 수업 시작 (로드맵 정밀 분석 & 어제 내용 복습 연계)
    // =================================================================================
    @Transactional
    public TutorDTO.ClassStartResponse startClass(Long userId, TutorDTO.ClassStartRequest request) {
        StudyPlanEntity plan = studyMapper.findById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        updatePersonaIfChanged(plan, request.personaName());

        // [1] 로드맵 분석: 오늘 주제 & 어제 주제 추출
        String todaysTopic = getTopicFromRoadmap(plan.getRoadmapJson(), request.dayCount());
        String yesterdayTopic = (request.dayCount() > 1) ? getTopicFromRoadmap(plan.getRoadmapJson(), request.dayCount() - 1) : "기초 오리엔테이션";

        // [2] 프롬프트 전략 수립: 단순 시작이 아니라 '연결'과 '목표 설정'
        String userPrompt = String.format("""
                [수업 컨텍스트]
                - 과목: %s (현재 레벨: %s)
                - **오늘의 핵심 주제**: %s
                - **어제 배운 내용**: %s
                - 학생 기분: %s
                - 학생 요청: "%s"
                
                [지시사항: 세계 최고의 강사처럼 오프닝]
                1. **브릿지(Bridge)**: 어제 배운 내용(%s)을 짧게 언급하며 오늘 내용(%s)과의 연관성을 설명해. (예: "어제 변수를 배웠죠? 오늘은 그 변수를 계산하는 연산자입니다.")
                2. **동기 부여**: 오늘 배울 내용이 왜 중요한지 실무적/학문적 가치를 한 문장으로 강조해.
                3. **스케줄링**: 학생 기분에 맞춰 학습 밀도(CLASS 시간)를 조절해. (좋음: 3000초, 나쁨: 1800초+휴식)
                
                [응답 형식]
                주제 | 오프닝 멘트 | {"CLASS": 3000, "BREAK": 600}
                """,
                plan.getGoal(), plan.getCurrentLevel(),
                todaysTopic, yesterdayTopic,
                request.dailyMood(),
                request.customOption() != null ? request.customOption() : "없음",
                extractTopicKeyword(yesterdayTopic), extractTopicKeyword(todaysTopic)
        );

        String systemPrompt = buildBaseSystemPrompt(plan, request.customOption()) +
                "\n너는 체계적이고 논리적인 '1타 강사'야. 흐름이 끊기지 않게 수업을 연결해.";

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

    // =================================================================================
    // 2. 세션 관리 (상황별 코칭)
    // =================================================================================
    @Transactional
    public TutorDTO.SessionStartResponse startSession(Long userId, TutorDTO.SessionStartRequest request) {
        String mode = request.sessionMode();
        String personaName = request.personaName();

        // 상황별 멘트 고도화
        String situation = switch (mode) {
            case "BREAK" -> "상황: 휴식 시간. 뇌과학적으로 휴식이 왜 기억 저장에 도움이 되는지 짧게 언급하며 쉬라고 해.";
            case "TEST" -> "상황: 테스트 시작. '틀려도 괜찮아, 모르는 걸 찾는 과정이야'라고 부담을 덜어주되 긴장감은 줘.";
            case "GRADING" -> "상황: 채점 중. AI가 꼼꼼하게 분석 중이라는 신뢰감을 주는 멘트를 해.";
            case "AI_FEEDBACK" -> "상황: 수업 종료. 오늘 배운 키워드 3가지를 해시태그처럼 말해주고, 내일 내용을 예고해줘.";
            default -> "상황: 수업 집중. 딴짓하지 말고 화면을 보라고 주의를 환기해.";
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
    // 3. 채팅 (영구 기억 + 적응형 티칭 + 소크라테스식 문답)
    // =================================================================================
    @Transactional
    public TutorDTO.FeedbackChatResponse adjustCurriculum(Long userId, Long planId, String message, boolean needsTts) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        // 1. 유저 메시지 DB 저장 (영구 기억 확보)
        chatMapper.saveMessage(planId, "USER", message);

        // 2. 대화 맥락 로드 (최근 50턴 - 충분한 컨텍스트)
        List<ChatMapper.ChatMessage> history = chatMapper.findRecentMessages(planId, 50);

        // 3. 교수법 전략 수립 (Adaptive Pedagogy)
        // 학생 레벨에 따라 설명의 깊이와 어조를 자동 조절
        String pedagogyStrategy = plan.getCurrentLevel().equalsIgnoreCase("BEGINNER")
                ? "쉬운 비유와 실생활 예시를 들어 설명해. 전문 용어는 최소화해."
                : "정확한 기술 용어를 사용하고, 원리와 내부 구조(Under the hood)를 깊게 설명해.";

        String basePrompt = commonMapper.findPromptContentByKey("TEACHER_" + plan.getPersona());
        if (basePrompt == null) basePrompt = "너는 열정적인 AI 선생님이야.";

        String teacherPrompt = String.format("""
            %s
            
            [현재 수업 정보]
            - 과목: %s
            - 학생 레벨: %s (목표: %s)
            - **교수법 전략**: %s
            
            [절대 규칙: World-Class Tutoring System]
            1. **문맥 완벽 유지**: 위 [대화 내역]을 분석해. 학생이 이전에 했던 질문이나 실수를 기억해서 "아까 말씀드린 것처럼~" 하고 연결해.
            2. **소크라테스식 검증**: 단순히 정답만 알려주지 마. 설명을 마친 후엔 반드시 **"그럼 이 경우에는 어떻게 될까요?"**라고 역질문을 던져 이해도를 체크해.
            3. **코드/예시 필수**: 코딩 질문이면 반드시 코드를, 이론 질문이면 반드시 예시를 들어.
            4. **잡담 차단**: 학생이 수업과 무관한 얘기를 하면 정중히 수업으로 복귀시켜.
            """,
                basePrompt, plan.getGoal(), plan.getCurrentLevel(), plan.getTargetLevel(), pedagogyStrategy);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(teacherPrompt));

        // 과거 대화 주입
        for (ChatMapper.ChatMessage chat : history) {
            if ("USER".equals(chat.sender())) {
                messages.add(new UserMessage(chat.message()));
            } else {
                messages.add(new AssistantMessage(chat.message()));
            }
        }

        // 4. AI 호출
        String aiResponse = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();

        // 5. AI 응답 DB 저장
        chatMapper.saveMessage(planId, "AI", aiResponse);

        String audioUrl = needsTts ? generateTtsAudio(aiResponse, plan.getPersona()) : null;
        return new TutorDTO.FeedbackChatResponse(aiResponse, audioUrl);
    }

    // =================================================================================
    // 4. 데일리 테스트 생성 (로드맵 내용 정밀 타격)
    // =================================================================================
    @Transactional(readOnly = true)
    public TutorDTO.DailyTestResponse generateTest(Long userId, Long planId, int dayCount) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String todaysTopic = getTopicFromRoadmap(plan.getRoadmapJson(), dayCount);

        String prompt = String.format("""
                [데일리 테스트 출제]
                - 과목: %s
                - 오늘 학습한 내용: %s
                - 난이도: %s 수준
                
                오늘 배운 '%s'의 핵심 개념을 확인하는 4지선다 퀴즈 1개를 JSON으로 출제해.
                단순 암기보다는 '이해했는지'를 묻는 함정 문제를 선호해.
                
                형식:
                {
                    "type": "QUIZ",
                    "question": "문제 지문",
                    "imageUrl": null,
                    "options": ["A", "B", "C", "D"],
                    "answerIndex": 0
                }
                """, plan.getGoal(), todaysTopic, plan.getCurrentLevel(), todaysTopic);

        String response = chatModel.call(prompt);
        String cleaned = cleanJson(response);

        try {
            return objectMapper.readValue(cleaned, TutorDTO.DailyTestResponse.class);
        } catch (Exception e) {
            return new TutorDTO.DailyTestResponse(
                    "QUIZ", "오늘 배운 내용을 복습해볼까요?", null,
                    List.of("네", "아니오", "글쎄요", "모르겠어요"), 0
            );
        }
    }

    // =================================================================================
    // 5. 실전 시험 및 채점 (전문가적 피드백)
    // =================================================================================
    @Transactional(readOnly = true)
    public TutorDTO.ExamGenerateResponse generateExam(Long userId, Long planId) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        StudyLogEntity lastLog = studyMapper.findLatestLogByPlanId(planId);
        String topic = (lastLog != null) ? lastLog.getContentSummary() : "기초 입문";

        // 멀티모달(이미지/코드) 시험 생성
        String promptText = String.format("""
            Role: Senior Examiner in %s.
            Topic: %s.
            
            Generate 2 high-quality questions in JSON format.
            - Question 1: Conceptual understanding (Multiple Choice).
            - Question 2: Practical application or Visual Analysis (Visual Analysis if Art/Bio, otherwise Code/Short Answer).
            """, plan.getGoal(), topic);

        String jsonResponse = chatModel.call(promptText);
        try {
            return objectMapper.readValue(cleanJson(jsonResponse), TutorDTO.ExamGenerateResponse.class);
        } catch (JsonProcessingException e) {
            return createFallbackExam(topic);
        }
    }

    @Transactional(readOnly = true)
    public TutorDTO.ExamGenerateResponse generateExam(Long userId, Long planId, int startDay, int endDay) {
        return generateExam(userId, planId);
    }

    public TutorDTO.ExamResultResponse submitExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        return evaluateExam(userId, request);
    }

    @Transactional
    public TutorDTO.ExamResultResponse evaluateExam(Long userId, TutorDTO.ExamSubmitRequest request) {
        StringBuilder summary = new StringBuilder();
        for (TutorDTO.ExamSubmitRequest.SubmittedAnswer ans : request.answers()) {
            summary.append(String.format("Q%d: %s ", ans.number(), ans.textAnswer()));
        }

        String prompt = String.format("""
            [채점 요청]
            학생 답안: %s
            
            엄격하게 채점하고, 틀린 부분은 '왜 틀렸는지'와 '올바른 접근법'을 구체적으로 피드백해.
            JSON: {"totalScore": 0, "isPassed": boolean, "aiComment": "총평", "feedbacks": []}
            """, summary.toString());

        String json = chatModel.call(prompt);
        try {
            return objectMapper.readValue(cleanJson(json), TutorDTO.ExamResultResponse.class);
        } catch(Exception e) {
            throw new TutorooException("채점 시스템 오류", ErrorCode.AI_PROCESSING_ERROR);
        }
    }

    // =================================================================================
    // 6. 유틸리티 및 헬퍼 메서드
    // =================================================================================

    // [New] 로드맵 파싱 로직 (정밀도 향상)
    private String getTopicFromRoadmap(String json, int dayCount) {
        if (!StringUtils.hasText(json)) return "심화 학습";
        try {
            JsonNode root = objectMapper.readTree(json);
            // detailedCurriculum이 Map 형태인지 Array 형태인지 유연하게 처리
            JsonNode curriculum = root.path("detailedCurriculum");

            if (curriculum.isObject()) {
                // "1주차": [...] 형태일 때
                for (JsonNode week : curriculum) {
                    if (week.isArray()) {
                        for (JsonNode dayPlan : week) {
                            String dayStr = dayPlan.path("day").asText();
                            // "3일차", "Day 3" 등의 문자열에서 숫자만 추출하여 비교
                            if (extractNumber(dayStr) == dayCount) {
                                return dayPlan.path("topic").asText() + " (" + dayPlan.path("method").asText() + ")";
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("로드맵 파싱 중 오류 (기본값 반환): {}", e.getMessage());
        }
        return "현재 진도에 맞는 심화 내용";
    }

    // 숫자 추출 유틸
    private int extractNumber(String text) {
        Matcher m = Pattern.compile("\\d+").matcher(text);
        return m.find() ? Integer.parseInt(m.group()) : -1;
    }

    private String extractTopicKeyword(String info) {
        return info.contains("(") ? info.substring(0, info.indexOf("(")).trim() : info;
    }

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
        return new TutorDTO.ExamGenerateResponse(topic + " 평가", List.of(new TutorDTO.ExamGenerateResponse.ExamQuestion(1, QuestionType.MULTIPLE_CHOICE, "개념 확인", null, null, List.of("O","X"), null)));
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

    @Transactional
    public TutorDTO.TestFeedbackResponse submitTest(Long userId, Long planId, String textAnswer, MultipartFile image) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        String prompt = "문제: " + plan.getGoal() + ". 답안: " + textAnswer + ". 점수(0~100)와 피드백.";
        String res = chatModel.call(prompt);
        int score = parseScore(res);
        studyMapper.saveLog(StudyLogEntity.builder().planId(planId).dayCount(0).testScore(score).aiFeedback(res).isCompleted(score >= 60).pointChange(score >= 60 ? 50 : 10).build());
        return new TutorDTO.TestFeedbackResponse(score, res, "요약", requestTts(res, plan.getPersona()), null, "화이팅", score >= 60);
    }

    public String convertSpeechToText(MultipartFile audio) {
        try {
            File temp = File.createTempFile("stt", ".webm");
            audio.transferTo(temp);
            String text = transcriptionModel.call(new AudioTranscriptionPrompt(new FileSystemResource(temp))).getResult().getOutput();
            temp.delete();
            return text;
        } catch (Exception e) { throw new TutorooException(ErrorCode.STT_PROCESSING_ERROR); }
    }

    @Transactional
    public void saveStudentFeedback(TutorDTO.TutorReviewRequest request) {
        studyMapper.updateStudentFeedback(request.planId(), request.dayCount(), request.feedback());
    }

    @Transactional
    public void renameCustomTutor(Long planId, String newName) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);
        plan.setCustomTutorName(newName);
        studyMapper.updatePlan(plan);
    }

    private record ParsedResponse(String topic, String aiMessage, Map<String, Integer> schedule) {}
    private String requestTts(String text, String persona) { return generateTtsAudio(text, persona); }
}