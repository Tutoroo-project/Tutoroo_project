package com.tutoroo.dto;

import lombok.Builder;
import java.util.List;

/**
 * [기능: 고도화된 대시보드 데이터 객체 (Record 변환 완료)]
 * 설명: 단순 지표를 넘어 AI 분석 리포트와 시각화용 통계 데이터를 포함합니다.
 */
@Builder
public record DashboardDTO(
        String name,                // 학생 이름
        String currentGoal,         // 현재 집중 목표
        double progressRate,        // 전체 진도율
        int currentPoint,           // 현재 보유 포인트
        int rank,                   // 전체 순위

        // --- [AI 분석 영역] ---
        String aiAnalysisReport,    // AI가 작성한 전문 분석 내용
        String aiSuggestion,        // AI의 향후 학습 제안

        // --- [시각화 데이터 영역] ---
        List<Integer> weeklyScores,   // 최근 7일간의 테스트 점수 추이 (차트용)
        List<String> recentFeedbacks  // 최근 AI 주요 피드백 리스트
) {
    // 참고: DashboardDTO는 그 자체가 핵심 정보이므로
    // 기존의 DashboardDTO 클래스 구조를 레코드로 완전 대체합니다.
}