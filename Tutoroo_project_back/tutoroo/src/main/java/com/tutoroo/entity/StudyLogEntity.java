package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDateTime;

/**
 * [기능: 데일리 학습 로그 엔티티]
 * 설명: 매일의 학습 결과, 테스트 점수, 그리고 복습용 요약본을 DB에 저장합니다.
 * 수정: 학생이 선생님에게 남긴 피드백(studentFeedback) 필드 추가 (Step 15)
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
    private Integer dayCount;       // N일차 표시

    private String contentSummary;  // 학습 내용 요약
    private String dailySummary;    // ★ 별표시가 포함된 상세 요약본 (복습용)

    private Integer testScore;      // 테스트 점수
    private String aiFeedback;      // AI -> 학생 피드백 (채점 결과)

    private String studentFeedback; // [추가] 학생 -> AI 선생님 피드백 (Step 15)

    private Integer pointChange;    // 획득 포인트
    private Boolean isCompleted;    // 학습 완료 여부
}