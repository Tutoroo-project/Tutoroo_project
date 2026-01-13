package com.tutoroo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

public class AssessmentDTO {

    /** userId 필드 제거 및 Lombok 적용 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultRequest {
        // userId 제거됨 (SecurityContext 사용)
        private String message;
        private List<Message> history;
        private boolean isStopRequested;
        private String goal;
        private String availableTime;
        private String targetDuration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultResponse {
        private String question;
        private String audioBase64;
        private int questionCount;
        private boolean isFinished;
    }

    /** userId 필드 제거 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadmapRequest {
        // userId 제거됨
        private String goal;
        private String teacherType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadmapResponse {
        private String summary;
        private Map<String, String> weeklyCurriculum;
        private List<String> examSchedule;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}