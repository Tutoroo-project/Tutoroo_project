package com.tutoroo.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    private Long id;
    private String username;
    private String password;
    private String name;
    private String gender;
    private Integer age;
    private String phone;
    private String email;
    private String profileImage;

    // 학부모 리포트용
    private String parentPhone;

    // OAuth2 정보
    private String provider;
    private String providerId;

    private String role;

    // [포인트 시스템]
    private Integer totalPoint;   // 누적 랭킹 포인트 (감소 X)
    private Integer pointBalance; // 사용 가능 포인트 (감소 O)

    private MembershipTier membershipTier; // 멤버십 등급 Enum

    private Integer dailyRank;
    private Integer level;
    private Integer exp;

    private Integer currentStreak;
    private LocalDate lastStudyDate;
    private Long rivalId;

    private String status;
    private String withdrawalReason;
    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- [편의 메서드] ---

    // 1. 이름 마스킹 (김철수 -> 김*수)
    public String getMaskedName() {
        if (name == null || name.length() < 2) return name;
        return name.charAt(0) + "*" + name.substring(2);
    }

    // 2. [중요] 유효 멤버십 조회 (Null-Safe)
    // PetService에서 user.getEffectiveTier().getAllowedPets() 호출 시 사용됨
    public MembershipTier getEffectiveTier() {
        return this.membershipTier != null ? this.membershipTier : MembershipTier.BASIC;
    }
}