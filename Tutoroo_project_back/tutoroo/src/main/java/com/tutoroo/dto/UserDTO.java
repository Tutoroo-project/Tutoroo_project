package com.tutoroo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * [기능: 회원 관련 데이터 전송 객체]
 */
public class UserDTO {

    // 1. 회원 정보 수정 요청
    public record UpdateRequest(
            String name,
            String phone,
            String parentPhone,
            Integer age,
            String email,
            String currentPassword,
            String newPassword
    ) {}

    // 2. 프로필 조회 응답 (Before/After 스냅샷 용도 겸용)
    @Builder
    public record ProfileInfo(
            String username,
            String email,
            String name,
            Integer age,
            String phone,
            String role,
            String profileImage,
            int point,
            String membershipTier,
            String provider // [New] 소셜 유저 여부 (google, kakao, null) - 프론트 UI 처리용
    ) {}

    // 3. 수정 후 응답 (변경 전/후 정보 포함)
    @Builder
    public record UpdateResponse(
            ProfileInfo before,
            ProfileInfo after,
            String message,
            String accessToken
    ) {}

    // 4. 회원 탈퇴 요청
    public record WithdrawRequest(
            String password,
            String reason
    ) {}

    // 5. 비밀번호 확인 요청
    public record PasswordVerifyRequest(
            String password
    ) {}

    // 6. 대시보드 정보 응답
    @Builder
    public record DashboardDTO(
            String name,
            String currentGoal,
            double progressRate,
            int currentPoint,
            int rank,
            String aiAnalysisReport,
            String aiSuggestion,
            List<Integer> weeklyScores,
            List<StudyDTO.StudySimpleInfo> studyList
    ) {}
    // 7. 결제 페이지용 간편 유저 정보
    @Builder
    public record PaymentUserInfo(
            Long id,        // 주문번호 생성용 (필수)
            String username,// 아이디
            String name,    // 이름
            String email,   // 이메일
            String phone,    // 전화번호
            String plan
    ) {}

    public record PasswordChangeRequest(
            String currentPassword,
            String newPassword,
            String confirmPassword
    ) {}
}