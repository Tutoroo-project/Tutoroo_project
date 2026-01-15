package com.tutoroo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetDiaryEntity {
    private Long id;
    private Long petId;
    private LocalDate date;
    private String content; // AI가 생성한 일기 내용
    private String mood;    // HAPPY, SAD, ANGRY
    private LocalDateTime createdAt;
}