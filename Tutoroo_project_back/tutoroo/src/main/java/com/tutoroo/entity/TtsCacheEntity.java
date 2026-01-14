package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsCacheEntity {
    private Long id;
    private String textHash;

    // [수정] 기존 Base64 데이터 대신 파일 경로(URL)만 저장
    // private String audioBase64; -> 삭제됨
    private String audioPath;

    private LocalDateTime createdAt;
}