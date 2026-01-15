package com.tutoroo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetInfoEntity {
    private Long petId;
    private Long userId;
    private String petName;

    // 기본 스탯
    private int fullness;
    private int intimacy;
    private int exp;

    // [신규] 심화 스탯 (High Quality)
    private int cleanliness;  // 위생 (0~100)
    private int stress;       // 스트레스 (0~100)
    private int energy;       // 에너지 (0~100)
    private boolean isSleeping; // 수면 상태

    // 성장 및 외형
    private int stage;
    private String petType;
    private String equippedItems; // JSON 문자열

    // 시간 정보
    private LocalDateTime lastFedAt;
    private LocalDateTime lastPlayedAt;
    private LocalDateTime lastCleanedAt; // [신규] 마지막 청소
    private LocalDateTime lastSleptAt;   // [신규] 잠든 시간
    private LocalDateTime birthDate;
    private LocalDateTime createdAt;
}