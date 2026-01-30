package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeLogEntity {
    private Long id;
    private Long userId;
    private Long questionId; // PracticeQuestionEntity FK

    private String userAnswer; // 사용자가 제출한 답 (텍스트, 파일URL)
    private Boolean isCorrect; // 정답 여부
    private String aiFeedback; // AI의 개별 첨삭

    private LocalDateTime solvedAt;
}