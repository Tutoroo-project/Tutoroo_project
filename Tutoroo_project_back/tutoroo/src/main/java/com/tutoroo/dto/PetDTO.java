package com.tutoroo.dto;

import lombok.Builder;

/**
 * [기능: 펫(다마고치) 관련 데이터 전송 객체 (Record 변환 완료)]
 * 설명: 펫의 상태 조회(Response)와 상호작용 요청(Request) 규격을 정의합니다.
 */
public class PetDTO {

    // 1. 펫 상태 응답
    @Builder
    public record PetStatusResponse(
            String petName,

            // --- [스탯 정보] ---
            int fullness,       // 배고픔 (0~100)
            int intimacy,       // 친밀도 (0~100)
            int exp,            // 경험치
            int cleanliness,    // 위생 (0~100, 똥 아이콘 표시용)
            int stress,         // 스트레스 (0~100, 표정 변화용)
            int energy,         // 에너지 (0~100)
            boolean isSleeping, // 수면 상태 여부 (Zzz 애니메이션용)

            // --- [외형 정보] ---
            int stage,          // 진화 단계 (1, 2, 3...)
            String petType,     // 펫 종류 (EGG, BABY_SLIME 등)

            // --- [메시지] ---
            String statusMessage // 펫이 유저에게 건네는 말
    ) {}

    // 2. 펫 상호작용 요청
    public record InteractionRequest(
            // 허용 값: FEED, PLAY, CLEAN, SLEEP, WAKE_UP
            String actionType
    ) {}
}