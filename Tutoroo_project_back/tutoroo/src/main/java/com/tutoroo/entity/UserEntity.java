package com.tutoroo.entity;

import lombok.*;
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

    // [OAuth2]
    private String provider;
    private String providerId;

    private String role;

    // [Membership & Point]
    private Integer totalPoint;
    private MembershipTier membershipTier;

    // [Ranking] ★ 신규 추가: 배치로 업데이트 되는 랭킹
    private Integer dailyRank;

    private Integer level;
    private Integer exp;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- 유틸 메서드 ---
    public String getMaskedName() {
        if (name == null || name.length() < 2) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    public MembershipTier getEffectiveTier() {
        if (this.membershipTier == null) return MembershipTier.BASIC;
        return this.membershipTier;
    }
}