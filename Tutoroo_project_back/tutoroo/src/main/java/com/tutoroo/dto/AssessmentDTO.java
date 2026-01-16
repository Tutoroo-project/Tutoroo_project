package com.tutoroo.dto;

import lombok.Builder;
import java.util.List;
import java.util.Map;

public class AssessmentDTO {

    // 1. 상담 요청
    @Builder
    public record ConsultRequest(
            String message,
            List<Message> history,
            boolean isStopRequested,
            String goal,
            String availableTime,
            String targetDuration
    ) {}

    // 2. 상담 응답
    @Builder
    public record ConsultResponse(
            String question,
            String audioUrl, // [최적화 변경] Base64 데이터 대신 파일 접근 URL 반환
            int questionCount,
            boolean isFinished
    ) {}

    // 3. 로드맵 생성 요청
    @Builder
    public record RoadmapRequest(
            String goal,

            // 선생님 타입 문자열 (예: "TIGER", "EASTERN_DRAGON")
            String teacherType
    ) {}

    // 4. 로드맵 응답
    @Builder
    public record RoadmapResponse(
            String summary,
            Map<String, String> weeklyCurriculum,
            List<String> examSchedule
    ) {}

    @Builder
    public record Message(String role, String content) {}
}