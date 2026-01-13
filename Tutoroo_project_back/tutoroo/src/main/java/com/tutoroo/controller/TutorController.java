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
    private final UserService userService; // 라이벌 매칭용

    // 1. 수업 시작
    @PostMapping("/class/start")
    public ResponseEntity<TutorDTO.ClassStartResponse> startClass(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.ClassStartRequest request
    ) {
        return ResponseEntity.ok(tutorService.startClass(user.userEntity().getId(), request));
    }

    // 2. 데일리 테스트 생성
    @GetMapping("/test/generate")
    public ResponseEntity<TutorDTO.DailyTestResponse> generateTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestParam int dayCount
    ) {
        return ResponseEntity.ok(tutorService.generateTest(user.userEntity().getId(), planId, dayCount));
    }

    // 3. 테스트 제출 및 채점
    @PostMapping(value = "/test/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorDTO.TestFeedbackResponse> submitTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("data") TutorDTO.TestSubmitRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(tutorService.submitTest(user.userEntity().getId(), request.planId(), request.textAnswer(), image));
    }

    // 4. 선생님 평가
    @PostMapping("/review")
    public ResponseEntity<String> reviewTutor(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.TutorReviewRequest request
    ) {
        tutorService.saveStudentFeedback(request);
        return ResponseEntity.ok("선생님에 대한 피드백이 반영되었습니다.");
    }

    // 5. 커리큘럼 조정 대화
    @PostMapping("/feedback/adjust")
    public ResponseEntity<TutorDTO.FeedbackChatResponse> adjustCurriculum(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.FeedbackChatRequest request
    ) {
        if (request.isFinished()) {
            return ResponseEntity.ok(tutorService.adjustCurriculum(user.userEntity().getId(), request.planId(), request.message()));
        }
        return ResponseEntity.ok(new TutorDTO.FeedbackChatResponse("피드백을 입력해주세요.", null));
    }

    // 6. 중간/기말고사 생성
    @GetMapping("/exam/generate")
    public ResponseEntity<TutorDTO.ExamGenerateResponse> generateExam(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestParam int startDay,
            @RequestParam int endDay
    ) {
        return ResponseEntity.ok(tutorService.generateExam(user.userEntity().getId(), planId, startDay, endDay));
    }

    // 7. 중간/기말고사 제출
    @PostMapping("/exam/submit")
    public ResponseEntity<TutorDTO.ExamResultResponse> submitExam(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.ExamSubmitRequest request
    ) {
        return ResponseEntity.ok(tutorService.submitExam(user.userEntity().getId(), request));
    }

    // [신규] 8. 음성 인식 (STT) - 마이크 버튼 누를 때 호출
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> convertSpeechToText(
            @RequestPart("audio") MultipartFile audio
    ) {
        return ResponseEntity.ok(tutorService.convertSpeechToText(audio));
    }

    // [신규] 9. 라이벌 매칭 요청
    @PostMapping("/rival/match")
    public ResponseEntity<String> matchRival(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.matchRival(user.userEntity().getId()));
    }
}