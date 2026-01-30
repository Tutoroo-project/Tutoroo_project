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

    // --- [인증 정보] ---
    private String username; // 아이디 (이메일과 동일)
    private String password;
    private String name;
    private String nickname; // [필수 추가] 이게 없으면 Mapper에서 에러 남!
    private String phone;
    private String email;
    private String gender;   // M, F
    private Integer age;

    // --- [프로필 & 미디어] ---
    private String profileImage;

    // --- [학부모 리포트용] ---
    private String parentPhone;

    // --- [OAuth2 소셜 로그인] ---
    private String provider;   // google, kakao 등
    private String providerId;

    private String role;       // ROLE_USER 등

    // --- [포인트 & 레벨] ---
    // NullPointerException 방지를 위해 기본값 0 권장, 하지만 Wrapper Class(Integer)도 OK
    private Integer totalPoint;   // 누적 랭킹 포인트
    private Integer pointBalance; // 사용 가능 포인트
    private Integer level;
    private Integer exp;

    private MembershipTier membershipTier; // 멤버십 등급 (Enum)

    // --- [랭킹 & 라이벌] ---
    private Integer dailyRank;
    private Long rivalId;

    // --- [학습 현황] ---
    private Integer currentStreak; // 연속 학습일
    private LocalDate lastStudyDate;

    // --- [상태 관리] ---
    private String status;           // ACTIVE, WITHDRAWN, BANNED
    private String withdrawalReason; // 탈퇴 사유
    private LocalDateTime deletedAt; // 탈퇴 일시

    // --- [메타 데이터] ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==========================================
    // [비즈니스 편의 메서드]
    // ==========================================

    // 1. 이름 마스킹 (김철수 -> 김*수, James -> J*s)
    public String getMaskedName() {
        if (name == null || name.length() < 2) {
            return name;
        }
        // 한국 이름(3글자)이나 영어 이름 모두 대응하기 위해
        // 첫 글자와 마지막 글자만 남기고 가운데를 *로 처리
        String first = name.substring(0, 1);
        String last = name.substring(name.length() - 1);
        return first + "*" + last;
    }

    // 2. 유효 멤버십 조회 (Null-Safe)
    public MembershipTier getEffectiveTier() {
        return this.membershipTier != null ? this.membershipTier : MembershipTier.BASIC;
    }

    // 3. 포인트 증가 (메모리 상에서 계산 필요 시 사용)
    public void addPoint(int amount) {
        if (this.totalPoint == null) this.totalPoint = 0;
        if (this.pointBalance == null) this.pointBalance = 0;
        if (this.exp == null) this.exp = 0;

        this.totalPoint += amount;
        this.pointBalance += amount;
        this.exp += amount;
    }
}