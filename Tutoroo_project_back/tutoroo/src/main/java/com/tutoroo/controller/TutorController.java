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

    // 1. 수업 시작 (페르소나 믹싱 및 오프닝)
    @PostMapping("/class/start")
    public ResponseEntity<TutorDTO.ClassStartResponse> startClass(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.ClassStartRequest request
    ) {
        return ResponseEntity.ok(tutorService.startClass(user.userEntity().getId(), request));
    }

    // 2. 데일리 테스트/미션 생성 (적응형 난이도)
    @GetMapping("/test/generate")
    public ResponseEntity<TutorDTO.DailyTestResponse> generateTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestParam int dayCount
    ) {
        return ResponseEntity.ok(tutorService.generateTest(user.userEntity().getId(), planId, dayCount));
    }

    // 3. 테스트 제출 및 피드백 (Vision AI & 텍스트 채점)
    // 주의: 프론트엔드에서 FormData로 'data'(JSON)와 'image'(File)를 보내야 함
    @PostMapping(value = "/test/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorDTO.TestFeedbackResponse> submitTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("data") TutorDTO.TestSubmitRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(tutorService.submitTest(
                user.userEntity().getId(),
                request.planId(),
                request.textAnswer(),
                image
        ));
    }

    // 4. 선생님 평가 (피드백 반영)
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
        return ResponseEntity.ok(tutorService.adjustCurriculum(
                user.userEntity().getId(),
                request.planId(),
                request.message()
        ));
    }

    // 6. 중간/기말고사 문제 생성
    @GetMapping("/exam/generate")
    public ResponseEntity<TutorDTO.ExamGenerateResponse> generateExam(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestParam int startDay,
            @RequestParam int endDay
    ) {
        return ResponseEntity.ok(tutorService.generateExam(user.userEntity().getId(), planId, startDay, endDay));
    }

    // 7. 중간/기말고사 제출 및 채점
    @PostMapping("/exam/submit")
    public ResponseEntity<TutorDTO.ExamResultResponse> submitExam(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.ExamSubmitRequest request
    ) {
        return ResponseEntity.ok(tutorService.submitExam(user.userEntity().getId(), request));
    }

    // 8. 음성 인식 (STT)
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> convertSpeechToText(
            @RequestPart("audio") MultipartFile audio
    ) {
        return ResponseEntity.ok(tutorService.convertSpeechToText(audio));
    }

    // 9. 라이벌 매칭 요청
    @PostMapping("/rival/match")
    public ResponseEntity<String> matchRival(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.matchRival(user.userEntity().getId()));
    }

    // 10. [신규] 커스텀 선생님 이름 변경 API
    @PatchMapping("/custom-name")
    public ResponseEntity<Void> renameCustomTutor(
            @RequestParam Long planId,
            @RequestParam String newName
    ) {
        tutorService.renameCustomTutor(planId, newName);
        return ResponseEntity.ok().build();
    }
}