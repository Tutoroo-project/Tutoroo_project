package com.tutoroo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * [기능: 사용자 관련 데이터 전송 객체 통합본]
 * 설명: 대시보드 정보 및 회원정보 수정 요청 규격을 정의합니다.
 */
public class UserDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        // --- [1. 보안 검증용 필수 필드] ---
        // 민감한 정보(아이디, 비번, 이메일, 폰)를 바꿀 땐 현재 비밀번호가 필수입니다.
        private String currentPassword;

        // --- [2. 변경할 정보 (입력된 값만 변경)] ---
        private String newUsername;     // 아이디 변경 (중복 체크 필요)
        private String newPassword;     // 새 비밀번호
        private String email;           // 이메일 변경
        private String phone;           // 휴대전화 번호 변경

        // 참고: 프로필 이미지는 JSON이 아닌 MultipartFile로 별도로 받습니다.
    }

    // (대시보드 DTO는 기존 유지)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardInfo {
        private String name;                // 사용자 이름
        private String currentGoal;         // 현재 목표
        private double progressRate;        // 진도율
        private int currentPoint;           // 포인트
        private int rank;                   // 랭킹
        private String aiAnalysisReport;    // AI 분석
        private String aiSuggestion;        // AI 제안
        private List<Integer> weeklyScores; // 주간 점수 그래프
    }
}