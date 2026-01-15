package com.tutoroo.dto;

import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * [기능: 학습 수준 파악 및 로드맵 관련 DTO (Record 변환 완료)]
 * 설명: AI와의 상담 대화 및 커리큘럼 생성에 필요한 데이터를 관리합니다.
 */
public class AssessmentDTO {

    // 1. 상담 요청 (AI와의 수준 파악 대화)
    @Builder
    public record ConsultRequest(
            String message,           // 사용자의 현재 답변
            List<Message> history,    // 이전 대화 기록
            boolean isStopRequested,  // 상담 중단 여부
            String goal,              // 학습 목표
            String availableTime,     // 학습 가능 시간
            String targetDuration     // 목표 기간
    ) {}

    // 2. 상담 응답
    @Builder
    public record ConsultResponse(
            String question,      // AI의 다음 질문
            String audioBase64,   // 질문 음성 데이터
            int questionCount,    // 현재 질문 번호
            boolean isFinished    // 상담 종료 여부
    ) {}

    // 3. 로드맵 생성 요청
    @Builder
    public record RoadmapRequest(
            String goal,          // 최종 목표
            String teacherType    // 선택한 선생님 스타일 (페르소나)
    ) {}

    // 4. 로드맵 응답 (AI가 생성한 커리큘럼)
    @Builder
    public record RoadmapResponse(
            String summary,                       // 로드맵 전체 요약
            Map<String, String> weeklyCurriculum, // 주차별 학습 내용
            List<String> examSchedule             // 시험 일정 리스트
    ) {}

    // 5. 상담용 메시지 객체
    public record Message(
            String role,    // user, assistant, system
            String content  // 메시지 내용
    ) {}
}