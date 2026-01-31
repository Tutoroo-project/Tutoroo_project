package com.tutoroo.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

public class StudyDTO {

    // =========================================================================
    // [1] Request DTOs (요청 데이터)
    // =========================================================================

    /**
     * [수정됨] 학습 플랜 생성 요청
     * - Service에서 request.goal(), request.teacherType() 호출 중
     */
    @Builder
    public record CreatePlanRequest(
            String goal,
            String teacherType // TIGER, RABBIT, TURTLE etc.
    ) {}

    /**
     * [수정됨] 간단 채팅 요청 (빨간줄 원인 해결)
     * - 기존: message만 있음
     * - 수정: planId 추가 (Controller에서 request.planId() 호출 지원)
     */
    @Builder
    public record SimpleChatRequest(
            Long planId,    // [필수 추가] 이게 있어야 빨간줄이 사라집니다.
            String message
    ) {}

    /**
     * [수정됨] 학습 로그 저장 요청
     */
    @Builder
    public record StudyLogRequest(
            Long planId,
            int dayCount,
            int score,
            String contentSummary,
            boolean isCompleted
    ) {}

    @Builder
    public record CalendarRequest(
            int year,
            int month
    ) {}

    // =========================================================================
    // [2] Response DTOs (응답 데이터)
    // =========================================================================

    /**
     * [대시보드 핵심] 학습 플랜 상세 정보
     * - AssessmentDTO.RoadmapData를 포함하여 로드맵 구조 전달
     */
    @Builder
    public record PlanDetailResponse(
            Long planId,
            String goal,
            String persona,
            String customTutorName,
            double progressRate,
            LocalDate startDate,
            LocalDate endDate,
            AssessmentDTO.RoadmapData roadmap, // AssessmentDTO가 같은 패키지면 import 불필요
            long daysRemaining
    ) {}

    /**
     * 학습 상태 요약 (메인 위젯용)
     */
    @Builder
    public record StudyStatusResponse(
            Long planId,
            String goal,
            String personaName,
            int currentDay,
            double progressRate,
            boolean isResting,
            String lastTopic
    ) {}

    /**
     * 채팅 응답
     */
    @Builder
    public record ChatResponse(
            String aiMessage,
            String audioUrl
    ) {}

    /**
     * 캘린더 응답
     */
    @Builder
    public record CalendarResponse(
            int year,
            int month,
            int totalStudyDays,
            List<DailyLog> logs
    ) {}

    @Builder
    public record DailyLog(
            int day,
            boolean isDone,
            int score,
            String topic
    ) {}

    /**
     * 학습 목록 간략 정보 (사이드바용)
     */
    @Builder
    public record StudySimpleInfo(
            Long id,
            String name,
            String tutor
    ) {}

    // =========================================================================
    // [3] Legacy DTOs (기존 코드 호환성 유지)
    // =========================================================================

    @Builder
    public record PlanResponse(
            Long planId,
            String subject,
            String curriculum,
            String message
    ) {}

    public record PlanRequest(
            String subject,
            String goal,
            String level
    ) {}

    @Builder
    public record DailySchedule(
            Long planId,
            String subject,
            String todaysTopic,
            boolean isCompleted
    ) {}

    public record ChatRequest(
            String message
    ) {}
}