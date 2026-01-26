package com.tutoroo.dto;

import java.util.List;
import java.util.Map;

public class TutorDTO {

    // 1. 수업 시작 요청
    public record ClassStartRequest(
            Long planId,
            int dayCount,
            String dailyMood,
            String personaName, // [필수] 선택한 선생님 (예: TURTLE)
            String customOption, // [New] 커스텀 요구사항 (예: "사투리로 설명해줘")
            Boolean needsTts    // [추가] TTS 생성 여부 (true일 때만 오디오 생성)
    ) {}

    // 2. 수업 시작 응답
    public record ClassStartResponse(
            String topic,
            String aiMessage,
            String audioUrl, // [최적화] Base64 -> URL
            String imageUrl,
            String backgroundMusicUrl,
            int gainedExp,
            int currentStreak,
            Map<String, Integer> schedule // [New] AI가 제안하는 세션별 시간표 (초 단위)
    ) {}

    // 3. 데일리 테스트 생성 응답
    public record DailyTestResponse(
            String type,     // QUIZ, MISSION 등
            String question,
            String imageUrl,
            String voiceUrl, // TTS 오디오 경로
            int timeLimitSeconds
    ) {}

    // 4. 테스트 제출 요청
    public record TestSubmitRequest(
            Long planId,
            String textAnswer,
            Boolean needsTts // [추가] TTS 생성 여부
    ) {}

    // 5. 테스트 피드백 응답
    public record TestFeedbackResponse(
            int score,
            String aiFeedback,
            String summary,
            String audioUrl,
            String explanationImageUrl,
            String nextMission,
            boolean isPassed
    ) {}

    // 6. 커리큘럼 조정 채팅 요청
    public record FeedbackChatRequest(
            Long planId,
            String message,
            Boolean needsTts // [추가] TTS 생성 여부
    ) {}

    // 7. 커리큘럼 조정 채팅 응답
    public record FeedbackChatResponse(
            String aiResponse,
            String audioUrl
    ) {}

    // 8. 선생님 평가 요청
    public record TutorReviewRequest(
            Long planId,
            int dayCount,
            String feedback,
            int rating
    ) {}

    // 9. 시험 생성 응답 (JSON 파싱용)
    public record ExamGenerateResponse(
            String title,
            List<ExamQuestion> questions
    ) {
        public record ExamQuestion(
                int number,
                String question,
                List<String> options
        ) {}
    }

    // 10. 시험 제출 요청
    public record ExamSubmitRequest(
            Long planId,
            List<Integer> answers // 객관식 답안 리스트
    ) {}

    // 11. 시험 결과 응답
    public record ExamResultResponse(
            int totalScore,
            int gainedLevel,
            String aiComment,
            List<String> wrongAnswerNotes,
            boolean isPassed
    ) {}
}