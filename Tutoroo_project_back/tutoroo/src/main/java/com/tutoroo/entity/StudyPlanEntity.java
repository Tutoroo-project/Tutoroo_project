package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [기능: 학습 플랜 엔티티]
 * 설명: 학생이 설정한 목표, AI 페르소나 스타일, 생성된 커리큘럼을 관리합니다.
 * 업데이트: 커스텀 튜터 이름(customTutorName) 필드가 추가되었습니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanEntity {
    private Long id;
    private Long userId;            // 외래키 (UserEntity.id)
    private String goal;            // 학습 목표 (예: 수능 만점, 기초 회화, 드로잉 등)

    private String persona;         // [Base] 1일차에 선택한 기본 동물 (호랑이, 토끼...)
    private String customTutorName; // [New] 학생이 지어준 커스텀 선생님 이름 (예: 김춘식)

    private String roadmapJson;     // AI가 생성한 로드맵 데이터 (Long Text)
    private LocalDate startDate;    // 학습 시작일
    private LocalDate endDate;      // 목표 기한
    private double progressRate;    // 현재 진도율 (0~100)

    private Boolean isPaid;         // 결제 여부 (구독/결제 모델 연동)
    private String status;          // PROCEEDING, COMPLETED, ABANDONED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // 정보 수정 시간 기록용

    /**
     * [도메인 로직: 남은 일수 계산]
     */
    public long getDaysRemaining() {
        if (endDate == null) return 0;
        return LocalDate.now().until(endDate).getDays();
    }
}