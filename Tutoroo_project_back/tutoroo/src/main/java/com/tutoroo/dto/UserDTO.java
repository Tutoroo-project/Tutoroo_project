package com.tutoroo.dto;

import lombok.Builder;

/**
 * [기능: 사용자 관련 데이터 전송 객체 통합본 (Record 변환 완료)]
 * 설명: 대시보드 정보 및 회원정보 수정 요청 규격을 정의합니다.
 * 변경사항: 내부 static class를 record로 변환하여 불변성을 확보했습니다.
 */
public class UserDTO {

    // 1. 회원 정보 수정 요청
    @Builder
    public record UpdateRequest(
            // --- [1. 보안 검증용 필수 필드] ---
            // 민감한 정보(아이디, 비번, 이메일, 폰)를 바꿀 땐 현재 비밀번호가 필수입니다.
            String currentPassword,

            // --- [2. 변경할 정보 (입력된 값만 변경)] ---
            String newUsername,     // 아이디 변경 (중복 체크 필요)
            String newPassword,     // 새 비밀번호
            String email,           // 이메일 변경
            String phone            // 휴대전화 번호 변경

            // 참고: 프로필 이미지는 JSON이 아닌 MultipartFile로 컨트롤러에서 별도로 받습니다.
    ) {}

    // 2. 대시보드 요약 정보 (UserDTO 내부에 정의된 버전)
    @Builder
    public record DashboardInfo(
            String name,                // 사용자 이름
            String currentGoal,         // 현재 목표
            double progressRate,        // 진도율
            int currentPoint,           // 포인트
            int rank,                   // 랭킹
            String aiAnalysisReport,    // AI 분석
            String aiSuggestion         // AI 제안
    ) {}
}