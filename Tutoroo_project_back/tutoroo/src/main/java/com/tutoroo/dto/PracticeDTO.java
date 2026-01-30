package com.tutoroo.dto;

import com.tutoroo.entity.QuestionType;
import lombok.Builder;
import java.util.List;

public class PracticeDTO {

    // 1. 테스트 생성 요청
    public record GenerateRequest(
            Long planId,
            int questionCount,   // 5, 10
            String difficulty,   // EASY, NORMAL, HARD
            boolean isWeaknessMode // true: 약점 공략 모드
    ) {}

    // 2. 테스트 생성 응답
    @Builder
    public record TestResponse(
            Long testSessionId,
            List<PracticeQuestion> questions
    ) {}

    @Builder
    public record PracticeQuestion(
            Long questionId,
            String topic,
            QuestionType type,
            String questionText,
            List<String> options, // 객관식 보기
            String referenceMediaUrl // [핵심] AI가 생성한 이미지 URL
    ) {}

    // 3. 답안 제출 요청
    public record SubmitRequest(
            Long planId,
            List<AnswerSubmission> answers
    ) {
        public record AnswerSubmission(
                Long questionId,
                String answerText, // 주관식/코드
                Integer selectedIndex // 객관식
        ) {}
    }

    // 4. 채점 결과 응답
    @Builder
    public record GradingResponse(
            int totalScore,
            String summaryReview,
            List<QuestionResult> results
    ) {
        public record QuestionResult(
                Long questionId,
                boolean isCorrect,
                String userAnswer,
                String explanation, // AI 해설
                String weaknessTag  // 약점 태그
        ) {}
    }

    // 5. 약점 분석 조회 응답
    @Builder
    public record WeaknessAnalysisResponse(
            List<WeakPoint> weakPoints,
            List<PracticeQuestion> recommendedQuestions
    ) {
        public record WeakPoint(
                String topic,
                int wrongCount,
                double errorRate
        ) {}
    }
}