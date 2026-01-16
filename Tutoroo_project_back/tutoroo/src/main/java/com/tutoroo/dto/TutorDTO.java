package com.tutoroo.dto;

import java.util.List;

public class TutorDTO {

    // 1. 수업 시작 요청
    public record ClassStartRequest(
            Long planId,
            int dayCount,
            String dailyMood,
            String personaName // [필수] 프론트에서 선택한 선생님 스타일
    ) {}

    // 2. 수업 시작 응답
    public record ClassStartResponse(
            String topic,
            String aiMessage,
            String audioUrl, // [최적화] Base64 -> URL
            String imageUrl,
            String backgroundMusicUrl,
            int gainedExp,
            int currentStreak
    ) {}

    // 3. 데일리 테스트 생성 응답
    public record DailyTestResponse(
            String type,     // QUIZ, MISSION 등
            String question,
            String imageUrl,
            String voiceUrl, // [확인] 이미 URL 네이밍을 사용 중이므로 유지 (내부 데이터는 파일 경로로 변경됨)
            int timeLimitSeconds
    ) {}

    // 4. 테스트 제출 요청
    public record TestSubmitRequest(
            Long planId,
            String textAnswer
    ) {}

    // 5. 테스트 피드백 응답
    public record TestFeedbackResponse(
            int score,
            String aiFeedback,
            String summary,
            String audioUrl, // [최적화] Base64 -> URL
            String explanationImageUrl,
            String nextMission,
            boolean isPassed
    ) {}

    // 6. 커리큘럼 조정 채팅 요청
    public record FeedbackChatRequest(
            Long planId,
            String message
    ) {}

    // 7. 커리큘럼 조정 채팅 응답
    public record FeedbackChatResponse(
            String aiResponse,
            String audioUrl // [최적화] Base64 -> URL
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