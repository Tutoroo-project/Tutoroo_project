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

    // 학부모 리포트용 (19세 미만 필수)
    private String parentPhone;

    // OAuth2 정보
    private String provider;
    private String providerId;

    private String role; // ROLE_USER, ROLE_GUEST

    // Membership & Point (Enum 타입 사용)
    private Integer totalPoint;
    private MembershipTier membershipTier;

    // Ranking & Stats
    private Integer dailyRank;
    private Integer level;
    private Integer exp;

    // Streak & Rival
    private Integer currentStreak;
    private LocalDate lastStudyDate;
    private Long rivalId;

    // [회원 상태 관리]
    private String status;           // ACTIVE, WITHDRAWN
    private String withdrawalReason; // 탈퇴 사유
    private LocalDateTime deletedAt; // 삭제 예정일

    // Time Stamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- [편의 메서드] ---

    // 이름 마스킹 (김*영)
    public String getMaskedName() {
        if (name == null || name.length() < 2) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    // [중요] 멤버십 등급 안전하게 가져오기 (Null이면 BASIC 반환)
    public MembershipTier getEffectiveTier() {
        if (this.membershipTier == null) return MembershipTier.BASIC;
        return this.membershipTier;
    }
}