package com.tutoroo.dto;

import java.util.List;

public class TutorDTO {

    public record ClassStartRequest(
            Long planId,
            int dayCount,
            String dailyMood
    ) {}

    public record ClassStartResponse(
            String topic,
            String personaMessage,
            String audioBase64,
            String imageUrl,
            String videoUrl,
            int studyMinutes,
            int breakMinutes
    ) {}

    public record DailyTestResponse(
            String testType,
            String content,
            String imageUrl,
            String videoUrl,
            int timeLimit
    ) {}

    public record TestSubmitRequest(
            Long planId,
            String textAnswer
    ) {}

    public record TestFeedbackResponse(
            int score,
            String explanation,
            String dailySummary,
            String audioBase64,
            String imageUrl,
            String videoUrl,
            boolean isPassed
    ) {}

    public record TutorReviewRequest(
            Long planId,
            int dayCount,
            String feedback
    ) {}

    public record FeedbackChatRequest(
            Long planId,
            String message,
            boolean isFinished
    ) {}

    public record FeedbackChatResponse(
            String message,
            String audioBase64
    ) {}

    // --- [신규] Step 19: 중간/기말고사 관련 DTO ---

    public record ExamQuestion(
            int number,         // 문제 번호
            String question,    // 지문
            List<String> options // 객관식 보기 (1~4번)
    ) {}

    public record ExamGenerateResponse(
            String title,       // 시험 제목 (예: 1주차 중간 점검)
            List<ExamQuestion> questions, // 문제 리스트
            int timeLimit       // 제한 시간 (초)
    ) {}

    public record ExamSubmitRequest(
            Long planId,
            List<Integer> answers // 학생이 고른 답 (순서대로 1, 3, 2, 4...)
    ) {}

    public record ExamResultResponse(
            int totalScore,     // 총점
            int rankChange,     // 예상 등수 변동 (재미 요소)
            String aiComment,   // AI 총평
            List<String> wrongAnswerExplanations, // 틀린 문제 해설
            boolean isPassed
    ) {}
}