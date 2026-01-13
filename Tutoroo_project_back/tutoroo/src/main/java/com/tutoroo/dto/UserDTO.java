package com.tutoroo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * [기능: 사용자 관련 데이터 전송 객체 통합본]
 * 설명: 대시보드 정보 요청/응답 및 마이페이지 정보 수정에 필요한 모든 구조를 포함합니다.
 */
public class UserDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String password; // [추가] 본인 확인을 위한 검증용 비밀번호
        private String phone;    // 수정 가능한 연락처 정보
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardInfo {
        private String name;                // 학생 이름
        private String currentGoal;         // 현재 집중 목표
        private double progressRate;        // 전체 진도율
        private int currentPoint;           // 현재 보유 포인트
        private int rank;                   // 전체 순위
        private String aiAnalysisReport;    // AI 분석 리포트
        private String aiSuggestion;        // AI 학습 제안
        private List<Integer> weeklyScores; // 최근 테스트 점수 추이
    }
}