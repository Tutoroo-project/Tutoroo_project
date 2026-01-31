package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeQuestionEntity {
    private Long id;
    private Long planId;

    private String contentHash; // 중복 방지 해시

    private String questionJson; // 문제 내용 원본 JSON
    private String topic;        // 주제
    private String questionType; // 문제 유형

    private Integer difficulty;  // 난이도
    private Integer correctRate; // 정답률

    private String imageUrl;     // [New] AI 생성 이미지 경로 저장 (비용 절감)

    private LocalDateTime createdAt;
}