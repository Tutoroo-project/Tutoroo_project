package com.tutoroo.service;

import com.tutoroo.dto.TutorDTO;
import com.tutoroo.entity.*;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.CommonMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorService {

    private final OpenAiChatModel chatModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final StudyMapper studyMapper;
    private final UserMapper userMapper;
    private final CommonMapper commonMapper; // [신규] 프롬프트/TTS 관리 매퍼

    // 1. 수업 시작
    @Transactional(readOnly = true)
    public TutorDTO.ClassStartResponse startClass(Long userId, TutorDTO.ClassStartRequest request) {
        UserEntity user = userMapper.findById(userId);
        StudyPlanEntity plan = studyMapper.findPlanById(request.planId());
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        String topic = request.dayCount() + "일차 수업";

        // [개선 1] DB에서 프롬프트 템플릿 가져오기
        String template = getPromptTemplate("CLASS_START");
        String prompt = String.format(template, plan.getPersona(), request.dayCount(), topic);

        String aiMessage = chatModel.call(prompt);

        // [개선 2] 캐싱 적용된 TTS 호출
        String audioBase64 = generateTieredTtsWithCache(aiMessage, user.getEffectiveTier());

        return new TutorDTO.ClassStartResponse(topic, aiMessage, audioBase64, 25, 5);
    }

    // 2. 데일리 테스트 생성
    @Transactional(readOnly = true)
    public TutorDTO.DailyTestResponse generateTest(Long userId, Long planId, int dayCount) {
        StudyPlanEntity plan = studyMapper.findPlanById(planId);

        String template = getPromptTemplate("TEST_GENERATE");
        String prompt = String.format(template, plan.getGoal(), dayCount);

        String question = chatModel.call(prompt);
        return new TutorDTO.DailyTestResponse("QUIZ", question, 300);
    }

    // 3. 테스트 제출 및 채점
    public TutorDTO.TestFeedbackResponse submitTest(Long userId, Long planId, String textAnswer, MultipartFile image) {
        UserEntity user = userMapper.findById(userId);

        String input = textAnswer + (image != null ? " [이미지 답안 포함]" : "");

        String template = getPromptTemplate("TEST_FEEDBACK");
        String prompt = String.format(template, input);

        String aiResponse = chatModel.call(prompt);
        int score = parseScore(aiResponse);
        String summary = aiResponse.contains("★") ? aiResponse.substring(aiResponse.indexOf("★")) : "요약 없음";

        // TTS (짧게 자름)
        String audioBase64 = generateTieredTtsWithCache(
                aiResponse.substring(0, Math.min(aiResponse.length(), 200)),
                user.getEffectiveTier()
        );

        boolean isPassed = saveTestResult(planId, score, aiResponse, summary);

        return new TutorDTO.TestFeedbackResponse(score, aiResponse, summary, audioBase64, isPassed);
    }

    @Transactional
    protected boolean saveTestResult(Long planId, int score, String aiFeedback, String summary) {
        int pointChange = (score >= 80) ? 100 : (score <= 40 ? -50 : 0);
        if (pointChange != 0) userMapper.updateUserPointByPlan(planId, pointChange);
        boolean isPassed = score >= 60;
        studyMapper.saveLog(StudyLogEntity.builder()
                .planId(planId).testScore(score).aiFeedback(aiFeedback).dailySummary(summary)
                .pointChange(pointChange).isCompleted(isPassed).build());
        return isPassed;
    }

    // 4. 커리큘럼 조정
    @Transactional(readOnly = true)
    public TutorDTO.FeedbackChatResponse adjustCurriculum(Long userId, Long planId, String message) {
        UserEntity user = userMapper.findById(userId);

        String template = getPromptTemplate("Chat_FEEDBACK");
        String prompt = String.format(template, message);

        String aiResponse = chatModel.call(prompt);
        String audioBase64 = generateTieredTtsWithCache(aiResponse, user.getEffectiveTier());

        return new TutorDTO.FeedbackChatResponse(aiResponse, audioBase64);
    }

    // --- [핵심 Private Helper Methods] ---

    /** 프롬프트 조회 (DB 없으면 기본값 사용 - 안전장치) */
    private String getPromptTemplate(String key) {
        String content = commonMapper.findPromptContentByKey(key);
        if (content == null) return "기본 프롬프트: " + key; // DB 초기화 안됐을 때 대비
        return content;
    }

    /** TTS 생성 (캐싱 적용) */
    private String generateTieredTtsWithCache(String text, MembershipTier tier) {
        if (text == null || text.isEmpty()) return null;

        // 1. 캐시 키 생성 (텍스트 + 목소리 조합의 해시)
        String voiceName = tier.getTtsVoice().toUpperCase();
        String uniqueKeySource = text + ":" + voiceName;
        String textHash = generateHash(uniqueKeySource);

        // 2. 캐시 조회
        TtsCacheEntity cached = commonMapper.findTtsCacheByHash(textHash);
        if (cached != null) {
            log.info("TTS Cache Hit! (Hash: {})", textHash);
            return cached.getAudioBase64();
        }

        // 3. 캐시 없음 -> API 호출
        log.info("TTS Cache Miss. Calling OpenAI API...");
        String audioBase64 = callOpenAiTts(text, tier);

        // 4. DB 저장 (비동기로 하면 더 좋지만 일단 동기로 저장)
        if (audioBase64 != null) {
            commonMapper.saveTtsCache(TtsCacheEntity.builder()
                    .textHash(textHash)
                    .audioBase64(audioBase64)
                    .build());
        }
        return audioBase64;
    }

    // 실제 OpenAI 호출 로직
    private String callOpenAiTts(String text, MembershipTier tier) {
        try {
            OpenAiAudioApi.SpeechRequest.Voice voiceEnum;
            try {
                voiceEnum = OpenAiAudioApi.SpeechRequest.Voice.valueOf(tier.getTtsVoice().toUpperCase());
            } catch (Exception e) {
                voiceEnum = OpenAiAudioApi.SpeechRequest.Voice.ALLOY;
            }

            // [수정] 메서드명을 소문자로 변경 (.model, .voice, .speed)
            OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
                    .model(tier.getTtsModel()) // .Model -> .model
                    .voice(voiceEnum)          // .Voice -> .voice
                    .speed(1.0f)               // .Speed -> .speed
                    .build();

            SpeechResponse res = speechModel.call(new SpeechPrompt(text, options));
            return Base64.getEncoder().encodeToString(res.getResult().getOutput());
        } catch (Exception e) {
            log.error("TTS API Error: {}", e.getMessage());
            return null;
        }
    }

    // SHA-256 해시 생성기
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
        } catch (Exception e) {
            return String.valueOf(input.hashCode()); // Fallback
        }
    }

    private int parseScore(String text) {
        try {
            Matcher m = Pattern.compile("(점수|Score)\\s*:\\s*(\\d{1,3})").matcher(text);
            if (m.find()) return Integer.parseInt(m.group(2));
        } catch (Exception e) {}
        return 50;
    }
}