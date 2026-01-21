package com.tutoroo.dto;

import lombok.Builder;
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
            Integer age,            // [Fix] UserService에서 사용하므로 추가
            String email,           // [Fix] UserService에서 사용하므로 추가
            String currentPassword,
            String newPassword
    ) {}

    // 2. 프로필 조회 응답 (Before/After 스냅샷 용도 겸용)
    @Builder
    public record ProfileInfo(
            String username,        // [Fix] 에러 원인: username 필드 추가
            String email,
            String name,
            Integer age,            // [Fix] UserService에서 사용하므로 추가
            String phone,
            String role,
            String profileImage,    // [Fix] UserService 변수명(profileImage)에 맞춤
            int point,
            String membershipTier   // [Fix] UserService 변수명(membershipTier)에 맞춤
    ) {}

    // 3. 수정 후 응답 (변경 전/후 정보 포함)
    @Builder
    public record UpdateResponse(
            ProfileInfo before,     // [Fix] 단순 id, name이 아닌 상세 비교 정보로 변경
            ProfileInfo after,      // [Fix] 상세 비교 정보
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
}