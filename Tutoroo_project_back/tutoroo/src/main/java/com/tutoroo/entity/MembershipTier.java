package com.tutoroo.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [기능: 멤버십 등급 정책 정의]
 * 설명: Basic, Standard, Premium 등급별 혜택을 코드로 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum MembershipTier {

    // 1. BASIC (무료)
    BASIC(1, "tts-1", "alloy", "SIMPLE"),

    // 2. STANDARD (9,900원)
    STANDARD(3, "tts-1", "shimmer", "WEEKLY"),

    // 3. PREMIUM (29,900원)
    PREMIUM(999, "tts-1-hd", "nova", "DEEP");

    private final int maxActiveGoals;       // 최대 학습 목표 개수
    private final String ttsModel;          // TTS 모델 (tts-1 vs tts-1-hd)
    private final String ttsVoice;          // TTS 성우 (alloy, shimmer, nova 등)
    private final String reportDetailLevel; // 리포트 상세도 (SIMPLE, WEEKLY, DEEP)
}