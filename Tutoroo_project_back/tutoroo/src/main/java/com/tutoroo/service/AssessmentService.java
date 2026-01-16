package com.tutoroo.service;

import com.tutoroo.dto.AssessmentDTO;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.util.FileStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final OpenAiChatModel chatModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final StudyMapper studyMapper;
    private final ObjectMapper objectMapper;
    private final FileStore fileStore; // [추가] 파일 저장소 유틸리티

    // 허용된 선생님 목록 (5마리 고정)
    private static final List<String> ALLOWED_TEACHERS = List.of(
            "TIGER", "RABBIT", "TURTLE", "KANGAROO", "EASTERN_DRAGON"
    );

    private static final int MAX_QUESTIONS = 30;

    @Transactional(readOnly = true)
    public AssessmentDTO.ConsultResponse consult(AssessmentDTO.ConsultRequest request) {
        // 1. 종료 요청이거나 질문 횟수 초과 시 상담 종료
        if (request.isStopRequested() || (request.history() != null && request.history().size() >= MAX_QUESTIONS)) {
            return AssessmentDTO.ConsultResponse.builder()
                    .question("상담이 종료되었습니다. 이제 학습 목표를 분석하여 로드맵을 생성해드릴게요. 잠시만 기다려주세요!")
                    .audioUrl(generateTtsAudio("상담이 종료되었습니다. 이제 학습 목표를 분석하여 로드맵을 생성해드릴게요."))
                    .isFinished(true)
                    .questionCount(request.history() != null ? request.history().size() : 0)
                    .build();
        }

        // 2. AI 상담 진행 (프롬프트는 DB나 코드 상수로 관리 권장하나, 여기서는 심플하게 구성)
        String systemPrompt = """
                너는 학습 상담사야. 학생의 목표(%s), 가용 시간(%s), 기간(%s)을 고려해서
                구체적인 커리큘럼을 짜기 위해 필요한 추가 질문을 **하나만** 해.
                말투는 친절하고 격려하는 어조로 해줘.
                """.formatted(request.goal(), request.availableTime(), request.targetDuration());

        String aiResponse = chatModel.call(systemPrompt);

        // 3. TTS 생성 (URL 반환)
        String audioUrl = generateTtsAudio(aiResponse);

        return AssessmentDTO.ConsultResponse.builder()
                .question(aiResponse)
                .audioUrl(audioUrl)
                .isFinished(false)
                .questionCount(request.history() != null ? request.history().size() + 1 : 1)
                .build();
    }

    @Transactional
    public AssessmentDTO.RoadmapResponse createStudentRoadmap(Long userId, AssessmentDTO.RoadmapRequest request) {
        try {
            // 1. 로드맵 생성 프롬프트
            String prompt = String.format("""
                    학생 목표: %s
                    선생님 스타일: %s
                    
                    위 정보를 바탕으로 4주 완성 학습 로드맵을 JSON으로 작성해.
                    형식:
                    {
                      "summary": "한 줄 요약",
                      "weeklyCurriculum": {
                        "1주차": "학습 내용",
                        "2주차": "학습 내용",
                        "3주차": "학습 내용",
                        "4주차": "학습 내용"
                      },
                      "examSchedule": ["1주차 주말 테스트", "4주차 최종 평가"]
                    }
                    JSON만 출력해.
                    """, request.goal(), request.teacherType());

            String json = chatModel.call(prompt);

            // Markdown 코드 블록 제거 (```json ... ```)
            if (json.startsWith("```")) {
                json = json.replaceAll("^```json", "").replaceAll("^```", "").trim();
            }

            // 2. JSON 파싱 및 응답 객체 생성
            AssessmentDTO.RoadmapResponse response = objectMapper.readValue(json, AssessmentDTO.RoadmapResponse.class);

            // [검증] 사용자가 선택한 선생님이 허용된 5마리인지 확인
            String selectedTeacher = request.teacherType();
            if (selectedTeacher == null || !ALLOWED_TEACHERS.contains(selectedTeacher.toUpperCase())) {
                log.warn("유효하지 않은 선생님 타입: {}. 기본값(TIGER)으로 설정합니다.", selectedTeacher);
                selectedTeacher = "TIGER";
            }

            StudyPlanEntity plan = StudyPlanEntity.builder()
                    .userId(userId)
                    .goal(request.goal())
                    .persona(selectedTeacher.toUpperCase()) // 검증된 문자열 저장
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
            // 1. OpenAI TTS 호출 (byte[] 반환)
            SpeechResponse response = speechModel.call(new SpeechPrompt(text));
            byte[] audioData = response.getResult().getOutput();

            // 2. [최적화] FileStore를 이용해 로컬 파일로 저장하고 URL 반환
            // 확장자는 .mp3 (OpenAI TTS 기본값)
            return fileStore.storeFile(audioData, ".mp3");

        } catch (Exception e) {
            log.error("TTS 생성 및 저장 오류: {}", e.getMessage());
            return null;
        }
    }
}