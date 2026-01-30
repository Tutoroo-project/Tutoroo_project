package com.tutoroo.dto;

import com.tutoroo.entity.QuestionType;
import java.util.List;
import java.util.Map;

/**
 * [기능: 수업 및 시험 데이터 전송 객체]
 * 업데이트: 그림, 노래, 영상 제출 등 모든 예체능 및 멀티모달 과외를 지원하도록 확장됨.
 */
public class TutorDTO {

    // 1. 수업 시작 요청
    public record ClassStartRequest(
            Long planId,
            int dayCount,
            String dailyMood,
            String personaName, // [필수] 선택한 선생님 (예: TURTLE)
            String customOption, // [New] 커스텀 요구사항 (예: "사투리로 설명해줘")
            boolean needsTts     // [New] TTS 생성 여부 플래그
    ) {}

    // 2. 수업 시작 응답
    public record ClassStartResponse(
            String topic,
            String aiMessage,
            String audioUrl, // [최적화] Base64 -> URL
            String imageUrl,
            String backgroundMusicUrl,
            int gainedExp,
            int currentStreak,
            Map<String, Integer> schedule // [New] AI가 제안하는 세션별 시간표 (초 단위)
    ) {}

    // 3. 데일리 테스트 생성 응답 [수정됨]
    // 서비스 로직(TutorService)과 필드를 일치시켰습니다.
    public record DailyTestResponse(
            String type,            // QUIZ, MISSION 등
            String question,        // 문제 내용
            String imageUrl,        // 이미지 (없으면 null)
            List<String> options,   // [New] 4지선다 보기 리스트 (기존 voiceUrl 대체)
            int answerIndex         // [New] 정답 인덱스 (기존 timeLimitSeconds 대체)
    ) {}

    // 4. 테스트 제출 요청 (데일리 테스트용)
    public record TestSubmitRequest(
            Long planId,
            String textAnswer
    ) {}

    // 5. 테스트 피드백 응답 (데일리 테스트용)
    public record TestFeedbackResponse(
            int score,
            String aiFeedback,
            String summary,
            String audioUrl,
            String explanationImageUrl,
            String nextMission,
            boolean isPassed
    ) {}

    // 6. 커리큘럼 조정 채팅 요청
    public record FeedbackChatRequest(
            Long planId,
            String message,
            boolean needsTts // [New] TTS 생성 여부 플래그
    ) {}

    // 7. 커리큘럼 조정 채팅 응답
    public record FeedbackChatResponse(
            String aiResponse,
            String audioUrl
    ) {}

    // 8. 선생님 평가 요청
    public record TutorReviewRequest(
            Long planId,
            int dayCount,
            String feedback,
            int rating
    ) {}

    // --- [확장된 시험 시스템 (멀티모달 지원)] ---

    // 9. 시험 생성 응답
    public record ExamGenerateResponse(
            String title,
            List<ExamQuestion> questions
    ) {
        public record ExamQuestion(
                int number,
                QuestionType type,    // [핵심] 문제 유형 (그림, 노래, 코딩, 객관식 등)
                String question,      // 문제 지문 (텍스트)

                // [Tutor -> Student] 문제에 포함된 자료 (듣기평가 파일, 참고 그림 등)
                String referenceMediaUrl,
                String referenceMediaType, // IMAGE, AUDIO, VIDEO, CODE

                List<String> options, // 객관식일 때만 존재 (그 외엔 null)

                // 코딩 문제일 경우 기본 코드 템플릿
                String codeTemplate
        ) {}
    }

    // 10. 시험 제출 요청
    public record ExamSubmitRequest(
            Long planId,
            List<SubmittedAnswer> answers
    ) {
        public record SubmittedAnswer(
                int number,

                // A. 텍스트/코딩/객관식 답안
                String textAnswer,
                Integer selectedOptionIndex, // 객관식용 (주관식은 null)

                // B. [Student -> Tutor] 파일 제출 답안 (그림, 노래, 영상)
                // 프론트엔드에서 파일을 먼저 업로드하고 받은 URL을 여기에 담아 보냅니다.
                String attachmentUrl
        ) {}
    }

    // 11. 시험 결과 응답 (첨삭 지도 강화)
    public record ExamResultResponse(
            int totalScore,
            int gainedLevel,
            String aiComment,
            List<QuestionFeedback> feedbacks,
            boolean isPassed
    ) {
        public record QuestionFeedback(
                int questionNo,
                boolean isCorrect,

                // 학생이 제출한 것 (텍스트 또는 파일 URL)
                String yourAnswer,
                String yourAttachmentUrl,

                // 정답 및 해설
                String correctAnswer, // 주관식은 모범 답안
                String explanation,

                // [AI 첨삭] 그림이나 노래에 대한 구체적 조언
                // 예: "고음 처리가 불안정해요", "원근법이 조금 어색하네요"
                String correctionDetail
        ) {}
    }

    // --- [New] 세션(모드) 변경 알림 요청 ---
    public record SessionStartRequest(
            Long planId,
            String sessionMode, // CLASS, BREAK, TEST, etc.
            String personaName,
            int dayCount,
            boolean needsTts
    ) {}

    // --- [New] 세션(모드) 변경 알림 응답 ---
    public record SessionStartResponse(
            String aiMessage,
            String audioUrl,
            String imageUrl // [New] 세션별 상황 이미지 (예: 쉬는시간 이미지)
    ) {}
}