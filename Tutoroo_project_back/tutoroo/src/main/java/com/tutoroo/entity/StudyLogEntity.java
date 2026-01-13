package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDateTime;

/**
 * [기능: 데일리 학습 로그 엔티티]
 * 설명: 매일의 학습 결과, 테스트 점수, 그리고 복습용 요약본을 DB에 저장합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyLogEntity {
    private Long id;                // 로그 PK
    private Long planId;            // 학습 플랜 FK
    private LocalDateTime studyDate;// 학습 일시
    private Integer dayCount;       // [신규] N일차 표시 (1일차, 2일차...)
    private String contentSummary;  // 일반적인 학습 요약
    private String dailySummary;    // [신규] 캘린더용 ★ 별표시가 포함된 상세 요약본
    private Integer testScore;      // 테스트 점수
    private String aiFeedback;      // AI의 테스트 해설
    private Integer pointChange;    // 획득/차감 포인트
    private Boolean isCompleted;    // 학습 완료 여부
}