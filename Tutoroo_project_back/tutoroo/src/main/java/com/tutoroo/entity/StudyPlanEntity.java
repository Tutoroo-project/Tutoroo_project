package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanEntity {

    private Long id;                // PK
    private Long userId;            // FK (사용자 ID)

    // --- [학습 목표 설정] ---
    private String goal;            // 예: "자바 백엔드 취업", "토익 900점"

    // 선생님 스타일 (TIGER, RABBIT, TURTLE 등)
    // - AssessmentService 상담 중엔 "DEFAULT" 였다가, 수업 시작 시 변경됨
    private String persona;

    private String customTutorName; // 사용자가 지어준 선생님 애칭
    private String customOption;
    // --- [AI 진단 & 로드맵 (핵심)] ---
    // MySQL: LONGTEXT, Postgres: TEXT (매우 긴 JSON 데이터 저장)
    private String roadmapJson;

    private String currentLevel;    // AI가 진단한 현재 레벨 (BEGINNER 등)
    private String targetLevel;     // 목표 레벨 (ADVANCED 등)

    // --- [스케줄 및 진도] ---
    private LocalDate startDate;
    private LocalDate endDate;

    // [수정] null 안전성을 위해 double -> Double 변경
    private Double progressRate;

    // --- [상태 관리] ---
    private Boolean isPaid;         // 유료 멤버십 플랜 여부
    private String status;          // "PROCEEDING", "COMPLETED", "STOPPED"

    // --- [메타 데이터] ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- [편의 메서드: 비즈니스 로직] ---

    // D-Day 계산
    public long getDaysRemaining() {
        if (endDate == null) return 0;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return Math.max(days, 0); // 음수 방지
    }

    // 진도율 100% 달성 여부 확인
    public boolean isCompleted() {
        return this.progressRate != null && this.progressRate >= 100.0;
    }

    // 학습 기간이 지났는지 확인
    public boolean isExpired() {
        return endDate != null && LocalDate.now().isAfter(endDate);
    }
}