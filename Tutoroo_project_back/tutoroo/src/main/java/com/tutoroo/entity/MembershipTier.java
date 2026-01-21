package com.tutoroo.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * [기능: 멤버십 등급 정의]
 * 설명: 등급별 혜택(선택 가능한 펫, TTS 품질, 졸업 보상, 최대 학습 목표 수)을 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum MembershipTier {

    // 1. BASIC (무료)
    // - 펫: 3종 (호랑이, 토끼, 거북이)
    // - 보상: 졸업 알 1개
    // - 학습 목표: 최대 1개 동시 진행 가능
    BASIC(
            1,
            "tts-1",
            "alloy",
            "SIMPLE",
            Set.of(PetType.TIGER, PetType.RABBIT, PetType.TURTLE),
            1,
            1 // [New] maxActiveGoals
    ),

    // 2. STANDARD (유료 - 9,900원)
    // - 펫: 7종 (Basic + 오소리, 쿼카, 캥거루, 동양용)
    // - 보상: 졸업 알 2개
    // - 학습 목표: 최대 3개 동시 진행 가능
    STANDARD(
            3,
            "tts-1",
            "shimmer",
            "WEEKLY",
            Set.of(PetType.TIGER, PetType.RABBIT, PetType.TURTLE,
                    PetType.HONEY_BADGER, PetType.QUOKKA, PetType.KANGAROO, PetType.EASTERN_DRAGON),
            2,
            3 // [New] maxActiveGoals
    ),

    // 3. PREMIUM (유료 - 29,900원)
    // - 펫: 모든 펫 (10종)
    // - 보상: 졸업 알 3개
    // - 학습 목표: 최대 10개 (사실상 무제한) 동시 진행 가능
    PREMIUM(
            5,
            "tts-1",
            "nova",
            "DAILY",
            Set.of(PetType.values()), // 모든 펫
            3,
            10 // [New] maxActiveGoals
    );

    private final int tierLevel;          // 등급 레벨 (높을수록 좋음)
    private final String ttsModel;        // TTS 모델 (표준/고품질)
    private final String ttsVoice;        // 기본 목소리
    private final String reportFrequency; // 리포트 주기
    private final Set<PetType> allowedPets; // 입양 가능한 펫 목록
    private final int graduationEggCount; // 졸업 시 보상 알 개수
    private final int maxActiveGoals;     // [필수] 동시 진행 가능한 최대 학습 목표 수
}