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

    // 1. 수업 시작 (DTO에 needsTts 포함됨)
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

    // 3. 테스트 제출 및 피드백 (수정됨: Service로 DTO 전체 전달)
    @PostMapping(value = "/test/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorDTO.TestFeedbackResponse> submitTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("data") TutorDTO.TestSubmitRequest request, // DTO 안에 needsTts 포함
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        // [수정] 기존에는 인자를 풀어서 넘겼으나, 이제 needsTts 확인을 위해 DTO 통째로 전달
        return ResponseEntity.ok(tutorService.submitTest(
                user.userEntity().getId(),
                request,
                image
        ));
    }

    // 4. 커리큘럼 조정 채팅 (수정됨: Service로 DTO 전체 전달)
    @PostMapping("/feedback/chat")
    public ResponseEntity<TutorDTO.FeedbackChatResponse> chatWithTutor(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.FeedbackChatRequest request
    ) {
        // [수정] needsTts 확인을 위해 DTO 통째로 전달
        return ResponseEntity.ok(tutorService.adjustCurriculum(
                user.userEntity().getId(),
                request
        ));
    }

    // 5. 튜터 평가 (기존 동일)
    @PostMapping("/review")
    public ResponseEntity<String> reviewTutor(@RequestBody TutorDTO.TutorReviewRequest request) {
        tutorService.saveStudentFeedback(request);
        return ResponseEntity.ok("피드백이 반영되었습니다.");
    }

    // 6. 시험 생성 (기존 동일)
    @GetMapping("/exam/generate")
    public ResponseEntity<TutorDTO.ExamGenerateResponse> generateExam(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestParam int startDay,
            @RequestParam int endDay
    ) {
        return ResponseEntity.ok(tutorService.generateExam(user.userEntity().getId(), planId, startDay, endDay));
    }

    // 7. 시험 제출 (기존 동일)
    @PostMapping("/exam/submit")
    public ResponseEntity<TutorDTO.ExamResultResponse> submitExam(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.ExamSubmitRequest request
    ) {
        return ResponseEntity.ok(tutorService.submitExam(user.userEntity().getId(), request));
    }

    // 8. STT 변환 (기존 동일)
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> convertSpeechToText(@RequestPart("audio") MultipartFile audio) {
        return ResponseEntity.ok(tutorService.convertSpeechToText(audio));
    }

    // 9. 라이벌 매칭 (기존 동일)
    @PostMapping("/rival/match")
    public ResponseEntity<String> matchRival(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.matchRival(user.userEntity().getId()));
    }

    // 10. 커스텀 튜터 이름 변경 (기존 동일)
    @PatchMapping("/custom-name")
    public ResponseEntity<Void> renameCustomTutor(
            @RequestParam Long planId,
            @RequestParam String newName
    ) {
        tutorService.renameCustomTutor(planId, newName);
        return ResponseEntity.ok().build();
    }
}