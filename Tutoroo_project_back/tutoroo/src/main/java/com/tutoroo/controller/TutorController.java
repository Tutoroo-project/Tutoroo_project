package com.tutoroo.controller;

import com.tutoroo.dto.TutorDTO;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.TutorService;
import com.tutoroo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;
    private final UserService userService;

    // 1. 수업 시작 (오프닝 + 스케줄 생성)
    @PostMapping("/class/start")
    public ResponseEntity<TutorDTO.ClassStartResponse> startClass(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.ClassStartRequest request
    ) {
        return ResponseEntity.ok(tutorService.startClass(user.getId(), request));
    }

    // 2. [New] 세션(모드) 변경 시 AI 멘트 요청 (BREAK, TEST 등)
    @PostMapping("/session/start")
    public ResponseEntity<TutorDTO.SessionStartResponse> startSession(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.SessionStartRequest request
    ) {
        return ResponseEntity.ok(tutorService.startSession(user.getId(), request));
    }

    // 3. 데일리 테스트 문제 생성
    @GetMapping("/test/generate")
    public ResponseEntity<TutorDTO.DailyTestResponse> generateTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestParam int dayCount
    ) {
        return ResponseEntity.ok(tutorService.generateTest(user.getId(), planId, dayCount));
    }

    // 4. 테스트 제출 및 채점
    @PostMapping(value = "/test/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorDTO.TestFeedbackResponse> submitTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("data") TutorDTO.TestSubmitRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(tutorService.submitTest(
                user.getId(),
                request.planId(),
                request.textAnswer(),
                image
        ));
    }

    // 5. AI와 채팅 (커리큘럼 조정 및 질의응답) - 이미지 지원 추가
    @PostMapping(value = "/feedback/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorDTO.FeedbackChatResponse> chatWithTutor(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("data") TutorDTO.FeedbackChatRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(tutorService.adjustCurriculum(
                user.getId(),
                request.planId(),
                request.message(),
                request.needsTts(),
                image
        ));
    }

    // 6. 튜터 평가 (학생 -> AI)
    @PostMapping("/review")
    public ResponseEntity<String> reviewTutor(@RequestBody TutorDTO.TutorReviewRequest request) {
        tutorService.saveStudentFeedback(request);
        return ResponseEntity.ok("피드백이 반영되었습니다.");
    }

    // 7. 주간/월간 시험 생성
    @GetMapping("/exam/generate")
    public ResponseEntity<TutorDTO.ExamGenerateResponse> generateExam(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestParam int startDay,
            @RequestParam int endDay
    ) {
        return ResponseEntity.ok(tutorService.generateExam(user.getId(), planId, startDay, endDay));
    }

    // 8. 시험 제출
    @PostMapping("/exam/submit")
    public ResponseEntity<TutorDTO.ExamResultResponse> submitExam(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.ExamSubmitRequest request
    ) {
        return ResponseEntity.ok(tutorService.submitExam(user.getId(), request));
    }

    // 9. STT (음성 -> 텍스트 변환)
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> convertSpeechToText(@RequestPart("audio") MultipartFile audio) {
        return ResponseEntity.ok(tutorService.convertSpeechToText(audio));
    }

    // 10. 라이벌 매칭
    @PostMapping("/rival/match")
    public ResponseEntity<String> matchRival(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.matchRival(user.getId()));
    }

    // 11. 커스텀 튜터 이름 변경
    @PatchMapping("/custom-name")
    public ResponseEntity<Void> renameCustomTutor(
            @RequestParam Long planId,
            @RequestParam String newName
    ) {
        tutorService.renameCustomTutor(planId, newName);
        return ResponseEntity.ok().build();
    }
}