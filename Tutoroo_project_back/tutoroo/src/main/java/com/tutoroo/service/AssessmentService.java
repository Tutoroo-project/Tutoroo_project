package com.tutoroo.service;

import com.tutoroo.dto.AssessmentDTO;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.mapper.StudyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final OpenAiChatModel chatModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final StudyMapper studyMapper;
    private final ObjectMapper objectMapper;

    private static final int MAX_QUESTIONS = 30;

    /**
     * [기능: 대화형 수준 파악 (TTS 포함)]
     */
    public AssessmentDTO.ConsultResponse consult(AssessmentDTO.ConsultRequest request) {
        int currentCount = (request.history() != null ? request.history().size() : 0) / 2 + 1;

        if (request.isStopRequested() || currentCount > MAX_QUESTIONS) {
            String msg = "수준 파악이 완료되었습니다. 로드맵을 생성하시겠습니까?";
            return AssessmentDTO.ConsultResponse.builder()
                    .question(msg)
                    .audioBase64(generateTtsAudio(msg))
                    .questionCount(currentCount)
                    .isFinished(true)
                    .build();
        }

        String systemPrompt = String.format("""
            당신은 입시 컨설턴트 AI입니다.
            학생 정보: [목표:%s, 시간:%s, 기간:%s]
            미션: 학생의 수준을 파악하기 위한 질문을 하세요. (%d번째 질문)
            """, request.goal(), request.availableTime(), request.targetDuration(), currentCount);

        String question;
        if (request.history() == null || request.history().isEmpty()) {
            question = chatModel.call(systemPrompt + " 첫 질문을 해주세요.");
        } else {
            String historyText = request.history().stream()
                    .map(m -> m.role() + ": " + m.content())
                    .collect(Collectors.joining("\n"));
            question = chatModel.call(systemPrompt + "\n대화내역:\n" + historyText);
        }

        String audioBase64 = generateTtsAudio(question);

        return AssessmentDTO.ConsultResponse.builder()
                .question(question)
                .audioBase64(audioBase64)
                .questionCount(currentCount)
                .isFinished(false)
                .build();
    }

    /**
     * [기능: 로드맵 생성 및 저장]
     * 성능 최적화: AI 호출은 트랜잭션 외부, 저장은 트랜잭션 내부에서 수행
     */
    public AssessmentDTO.RoadmapResponse createStudentRoadmap(Long userId, AssessmentDTO.RoadmapRequest request) {
        String prompt = String.format("""
            목표: %s
            요청: 주차별 커리큘럼 JSON 생성
            형식: {"summary":"...", "weeklyCurriculum":{"1주차:..."}, "examSchedule":[]}
            JSON만 응답해.
            """, request.goal());

        // 1. AI 호출 (오래 걸림 - No Transaction)
        String json = chatModel.call(prompt);

        // 2. 파싱 및 DB 저장 (Transaction)
        return saveRoadmap(userId, request, json);
    }

    @Transactional
    protected AssessmentDTO.RoadmapResponse saveRoadmap(Long userId, AssessmentDTO.RoadmapRequest request, String json) {
        try {
            // JSON 마크다운 제거 (```json ... ```)
            if (json.startsWith("```")) {
                json = json.replaceAll("^```json", "").replaceAll("```$", "").trim();
            }

            AssessmentDTO.RoadmapResponse response = objectMapper.readValue(json, AssessmentDTO.RoadmapResponse.class);

            StudyPlanEntity plan = StudyPlanEntity.builder()
                    .userId(userId)
                    .goal(request.goal())
                    .persona(request.teacherType())
                    .roadmapJson(json)
                    .progressRate(0.0)
                    .status("PROCEEDING")
                    .isPaid(false)
                    .build();

            studyMapper.savePlan(plan);
            return response;
        } catch (Exception e) {
            log.error("로드맵 파싱/저장 실패: {}", e.getMessage());
            throw new RuntimeException("로드맵 생성 중 오류가 발생했습니다.");
        }
    }

    private String generateTtsAudio(String text) {
        try {
            SpeechResponse response = speechModel.call(new SpeechPrompt(text));
            return Base64.getEncoder().encodeToString(response.getResult().getOutput());
        } catch (Exception e) {
            log.error("TTS 오류: {}", e.getMessage());
            return null;
        }
    }
}