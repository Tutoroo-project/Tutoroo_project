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

    // [신규] 학부모 리포트용
    private String parentPhone;

    // [OAuth2]
    private String provider;
    private String providerId;

    private String role;

    // [Membership & Point]
    private Integer totalPoint;
    private MembershipTier membershipTier;

    // [Ranking]
    private Integer dailyRank;

    private Integer level;
    private Integer exp;

    // [신규] 스트릭 & 라이벌 시스템
    private Integer currentStreak; // 연속 학습일 (잔디)
    private LocalDate lastStudyDate; // 마지막 학습 날짜 (스트릭 계산용)
    private Long rivalId;          // 현재 지정된 라이벌 ID

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