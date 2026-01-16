package com.tutoroo.dto;

import lombok.Builder;

/**
 * [기능: 사용자 관련 데이터 전송 객체 (Record)]
 * 설명: 회원 정보 수정, 대시보드 조회, 탈퇴 요청 등에 사용됩니다.
 */
public class UserDTO {

    // 1. 회원 정보 수정 요청
    @Builder
    public record UpdateRequest(
            String currentPassword, // 보안 검증용 (필수)
            String newUsername,     // 변경할 아이디 (선택)
            String newPassword,     // 변경할 비밀번호 (선택)
            String email,           // 변경할 이메일 (선택)
            String phone            // 변경할 전화번호 (선택)
    ) {}

    // 2. 대시보드 응답 DTO
    @Builder
    public record DashboardDTO(
            String name,                // 사용자 이름
            String currentGoal,         // 현재 진행 중인 목표
            double progressRate,        // 진행률 (%)
            int currentPoint,           // 보유 포인트
            int rank,                   // 현재 랭킹
            String aiAnalysisReport,    // AI 분석 리포트
            String aiSuggestion,        // AI 추천 활동
            java.util.List<Integer> weeklyScores // 최근 7일 성적 그래프용 데이터
    ) {}

    // 3. 회원 탈퇴 요청
    @Builder
    public record WithdrawRequest(
            String password,    // 본인 확인용 비밀번호
            String reason       // 탈퇴 사유
    ) {}
}