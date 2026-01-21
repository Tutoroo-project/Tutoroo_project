package com.tutoroo.dto;

import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * [기능: 수준 진단 및 학습 로드맵 데이터 전송 객체]
 * 업데이트: AssessmentService의 심층 상담 로직 및 AI JSON 파싱 구조와 완벽 동기화됨.
 */
public class AssessmentDTO {

    // --- [1] 상담(Consultation) 요청/응답 ---

    @Builder
    public record StudyStartRequest(
            String goal,           // 학습 목표
            String deadline,       // 목표 기한
            String availableTime,  // 하루 가용 시간
            String teacherType     // 선택한 선생님 페르소나 (TIGER, RABBIT...)
    ) {}

    @Builder
    public record ConsultRequest(
            StudyStartRequest studyInfo,
            List<Message> history,   // 이전 대화 내역
            String lastUserMessage   // 사용자의 최신 답변
    ) {}

    @Builder
    public record ConsultResponse(
            String aiMessage,      // AI의 질문 또는 답변
            String audioUrl,       // TTS 오디오 URL (없으면 null)
            boolean isFinished     // 상담 종료 여부 (true면 로드맵 생성 요청 보내야 함)
    ) {}

    // --- [2] 로드맵 생성 요청/응답 ---

    @Builder
    public record AssessmentSubmitRequest(
            StudyStartRequest studyInfo,
            List<Message> history // 상담 완료 후 전체 대화 내역 전송
    ) {}

    // [대시보드/상담결과 공용] 최종 결과 응답
    @Builder
    public record AssessmentResultResponse(
            Long planId,            // 생성된 플랜 ID
            String analyzedLevel,   // 분석된 레벨 (BEGINNER, INTERMEDIATE, ADVANCED)
            String analysisReport,  // AI 분석 코멘트
            RoadmapOverview overview, // 화면 표시용 요약 정보 (목차 등)
            String message
    ) {}

    // --- [3] AI JSON 매핑용 데이터 구조 (DB 저장용) ---
    // AssessmentService에서 ObjectMapper로 파싱할 때 사용됩니다.

    @Builder
    public record RoadmapData(
            String summary,
            List<Chapter> tableOfContents,                      // 주차별/챕터별 목차
            Map<String, List<DailyDetail>> detailedCurriculum,  // 상세 스케줄 (Key: "1주차")
            List<String> examSchedule                           // 시험 일정
    ) {}

    // 로드맵 목차 정보
    public record Chapter(
            String week,        // "1주차"
            String title,       // "변수와 자료형"
            String description  // "기초 문법을 완벽하게 이해합니다."
    ) {}

    // 로드맵 상세 정보 (일자별)
    public record DailyDetail(
            String day,         // "1일차"
            String topic,       // "변수 선언법"
            String method,      // "강의 수강"
            String material     // "1장 교재"
    ) {}

    // --- [4] 화면 표시용 요약 객체 ---
    @Builder
    public record RoadmapOverview(
            String summary,
            List<Chapter> chapters // detailedCurriculum은 너무 크므로 제외하고 목차만 전달
    ) {}

    // --- [5] 기존 StudyController 간편 생성 호환용 ---

    @Builder
    public record RoadmapRequest(
            String goal,
            String teacherType,
            String currentLevel // null 허용 (없으면 BEGINNER 처리)
    ) {}

    @Builder
    public record RoadmapResponse(
            String summary,
            Map<String, String> weeklyCurriculum, // (간편 버전용) 단순화된 커리큘럼
            List<String> examSchedule
    ) {}

    // --- [6] 공통 유틸 ---
    @Builder
    public record Message(
            String role,   // "user", "assistant", "system"
            String content
    ) {}

    // --- [7] 레벨 테스트 (기존 기능 유지) ---
    @Builder public record LevelTestRequest(String subject) {}

    @Builder public record LevelTestResponse(
            String testId,
            String subject,
            List<TestQuestion> questions
    ) {
        public record TestQuestion(int questionNo, String question, List<String> options) {}
    }

    @Builder public record TestSubmitRequest(
            String testId,
            String subject,    // 과목명 검증용
            List<Integer> answers // 사용자가 선택한 답안 인덱스 리스트 (0~4)
    ) {}

    @Builder public record AssessmentResult(
            String level,
            int score,
            String analysis,
            String recommendedPath
    ) {}
}