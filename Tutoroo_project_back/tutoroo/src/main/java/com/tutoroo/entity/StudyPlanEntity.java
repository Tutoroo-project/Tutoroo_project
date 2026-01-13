package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [기능: 학습 플랜 엔티티]
 * 설명: 학생이 설정한 목표, AI 페르소나 스타일, 생성된 커리큘럼을 관리합니다.
 * 작동원리: 한 명의 유저가 여러 목표를 가질 수 있으며 결제 여부(isPaid)에 따라 추가 가능 여부를 결정합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanEntity {
    private Long id;
    private Long userId;            // 외래키 (UserEntity.id)
    private String goal;            // 학습 목표 (예: 수능 만점, 기초 회화 등)
    private String persona;         // AI 스타일 (호랑이, 거북이, 토끼, 캥거루, 드래곤)
    private String roadmapJson;     // AI가 생성한 로드맵 데이터 (Long Text)
    private LocalDate startDate;    // 학습 시작일
    private LocalDate endDate;      // 목표 기한 (캘린더 연동)
    private double progressRate;   // 현재 진도율 (0~100)
    private Boolean isPaid;         // 다중 목표 설정을 위한 결제 여부
    private String status;          // PROCEEDING(진행중), COMPLETED(완료), ABANDONED(중단)
    private LocalDateTime createdAt;

    /**
     * [도메인 로직: 남은 일수 계산]
     */
    public long getDaysRemaining() {
        if (endDate == null) return 0;
        return LocalDate.now().until(endDate).getDays();
    }
}