package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanEntity {
    private Long id;
    private Long userId;
    private String goal;

    // 선생님 타입 (예: "TIGER", "KANGAROO")
    private String persona;

    private String customTutorName; // 선생님 애칭

    private String roadmapJson;
    private LocalDate startDate;
    private LocalDate endDate;
    private double progressRate;

    // [필수 추가] 레벨 기반 학습 로드맵 생성을 위한 필드
    private String currentLevel;
    private String targetLevel;

    private Boolean isPaid;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 편의 메서드: 남은 일수 계산
    public long getDaysRemaining() {
        if (endDate == null) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return days < 0 ? 0 : days;
    }
}