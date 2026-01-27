package com.tutoroo.dto;

import lombok.Builder;
import java.util.List;
import java.time.LocalDate;

public class StudyDTO {

    // --- 캘린더 관련 ---
    @Builder
    public record CalendarRequest(int year, int month) {}

    @Builder
    public record CalendarResponse(int year, int month, int totalStudyDays, List<DailyLog> logs) {}

    @Builder
    public record DailyLog(int day, boolean isDone, int score, String topic) {}

    // --- [대시보드 핵심] 학습 플랜 상세 정보 ---
    @Builder
    public record PlanDetailResponse(
            Long planId,
            String goal,
            String persona,
            String customTutorName,
            double progressRate,
            LocalDate startDate,
            LocalDate endDate,

            // [진짜 빙산] 상세 로드맵 데이터 (RoadmapData 구조 그대로 반환)
            AssessmentDTO.RoadmapData roadmap,

            long daysRemaining
    ) {}

    // --- 학습 상태 및 로그 ---
    @Builder
    public record StudyStatusResponse(
            Long planId, String goal, String personaName, int currentDay,
            double progressRate, boolean isResting, String lastTopic
    ) {}

    public record CreatePlanRequest(String goal, String teacherType) {}

    public record StudyLogRequest(Long planId, int dayCount, int score, String contentSummary, boolean isCompleted) {}

    public record SimpleChatRequest(String message) {}

    @Builder
    public record ChatResponse(String aiMessage, String audioUrl) {}

    @Builder
    public record StudySimpleInfo(Long id, String name, String tutor) {}

    // 기존에 있었던 PlanResponse (단순 생성용) 유지
    @Builder
    public record PlanResponse(
            Long planId,
            String subject,
            String curriculum,
            String message
    ) {}

    // 기존에 있었던 PlanRequest (단순 생성용) 유지
    public record PlanRequest(
            String subject,
            String goal,
            String level
    ) {}

    // 기존에 있었던 DailySchedule 유지
    @Builder
    public record DailySchedule(
            Long planId,
            String subject,
            String todaysTopic,
            boolean isCompleted
    ) {}

    // 기존에 있었던 ChatRequest 유지
    public record ChatRequest(
            String message
    ) {}
}