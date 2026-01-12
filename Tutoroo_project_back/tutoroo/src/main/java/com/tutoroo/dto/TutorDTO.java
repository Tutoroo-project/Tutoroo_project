package com.tutoroo.dto;

public class TutorDTO {

    /** [기능: 수업 시작 요청] - userId 제거됨 */
    public record ClassStartRequest(
            Long planId,
            int dayCount
    ) {}

    /** [기능: 수업 시작 응답 (음성 포함)] */
    public record ClassStartResponse(
            String topic,
            String personaMessage,
            String audioBase64,
            int studyMinutes,
            int breakMinutes
    ) {}

    /** [기능: 데일리 테스트 생성 응답] */
    public record DailyTestResponse(
            String testType,
            String content,
            int timeLimit
    ) {}

    /** [기능: 테스트 답안 제출] - userId 제거됨 */
    public record TestSubmitRequest(
            Long planId,
            String textAnswer
    ) {}

    /** [기능: 테스트 피드백 응답 (음성 포함)] */
    public record TestFeedbackResponse(
            int score,
            String explanation,
            String dailySummary,
            String audioBase64,
            boolean isPassed
    ) {}

    /** [기능: 피드백 대화 요청] - userId 제거됨 */
    public record FeedbackChatRequest(
            Long planId,
            String message,
            boolean isFinished
    ) {}

    /** [기능: 피드백 대화 응답 (음성 포함)] */
    public record FeedbackChatResponse(
            String message,
            String audioBase64
    ) {}
}