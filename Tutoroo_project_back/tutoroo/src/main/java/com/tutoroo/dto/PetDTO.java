package com.tutoroo.dto;

import lombok.Builder;
import java.util.List;

public class PetDTO {

    // 1. 펫 상태 응답
    @Builder
    public record PetStatusResponse(
            Long petId,
            String petName,
            int fullness,
            int intimacy,
            int exp,
            int maxExp,
            int cleanliness,
            int stress,
            int energy,
            boolean isSleeping,
            int stage,
            String petType,
            String customImageUrl, // [New] 커스텀 이미지
            String status,
            String statusMessage
    ) {}

    // 2. 펫 상호작용 요청
    public record InteractionRequest(
            String actionType // FEED, PLAY, CLEAN, SLEEP, WAKE_UP
    ) {}

    // 3. 입양 가능한 펫 목록 응답
    @Builder
    public record AdoptableListResponse(
            List<PetSummary> availablePets,
            String message
    ) {}

    @Builder
    public record PetSummary(
            String type,
            String name,
            String description
    ) {}

    // 4. 초기 입양 요청
    public record InitialAdoptRequest(
            String petType,
            String petName
    ) {}

    // 5. 졸업 후 랜덤 알 후보 응답
    @Builder
    public record RandomEggResponse(
            List<PetSummary> candidates,
            int choiceCount
    ) {}

    // 6. 알 선택 및 부화 요청
    public record EggSelectRequest(
            String selectedPetType,
            String petName
    ) {}

    // 7. [수정] 커스텀 펫 생성 요청 (변수명 Entity와 통일)
    public record CustomPetCreateRequest(
            String petName,           // 기존 name -> petName 수정
            String customDescription, // 기존 description -> customDescription 수정
            String baseType           // 프롬프트 보조용 (예: CAT)
    ) {}

    // 8. [New] 일기 목록 조회용 응답 DTO
    @Builder
    public record PetDiaryResponse(
            Long diaryId,
            String date,     // 날짜 (2026-02-02)
            String content,  // 일기 내용
            String mood      // 기분 (HAPPY 등)
    ) {}
}