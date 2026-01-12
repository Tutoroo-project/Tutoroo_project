package com.tutoroo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * [기능: 고도화된 대시보드 데이터 객체]
 * 설명: 단순 지표를 넘어 AI 분석 리포트와 시각화용 통계 데이터를 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private String name;                // 학생 이름
    private String currentGoal;         // 현재 집중 목표
    private double progressRate;        // 전체 진도율
    private int currentPoint;           // 현재 보유 포인트
    private int rank;                   // 전체 순위

    // AI 분석 영역
    private String aiAnalysisReport;    // AI가 작성한 전문 분석 내용
    private String aiSuggestion;        // AI의 향후 학습 제안

    // 시각화 데이터 영역
    private List<Integer> weeklyScores; // 최근 7일간의 테스트 점수 추이
    private List<String> recentFeedbacks; // 최근 AI 주요 피드백 리스트
}