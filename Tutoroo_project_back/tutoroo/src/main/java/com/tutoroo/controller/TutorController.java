package com.tutoroo.controller;

import com.tutoroo.dto.TutorDTO;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.TutorService;
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

    // 1. 수업 시작
    @PostMapping("/class/start")
    public ResponseEntity<TutorDTO.ClassStartResponse> startClass(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.ClassStartRequest request
    ) {
        // [수정] user.getUserEntity() -> user.userEntity() (Record 문법)
        return ResponseEntity.ok(tutorService.startClass(user.userEntity().getId(), request));
    }

    // 2. 데일리 테스트 생성
    @GetMapping("/test/generate")
    public ResponseEntity<TutorDTO.DailyTestResponse> generateTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestParam int dayCount
    ) {
        // [수정] user.getUserEntity() -> user.userEntity()
        return ResponseEntity.ok(tutorService.generateTest(user.userEntity().getId(), planId, dayCount));
    }

    // 3. 테스트 제출 및 채점
    @PostMapping(value = "/test/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorDTO.TestFeedbackResponse> submitTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("data") TutorDTO.TestSubmitRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        // [수정] user.getUserEntity() -> user.userEntity()
        return ResponseEntity.ok(tutorService.submitTest(user.userEntity().getId(), request.planId(), request.textAnswer(), image));
    }

    // 4. 커리큘럼 조정 (피드백)
    @PostMapping("/feedback/adjust")
    public ResponseEntity<TutorDTO.FeedbackChatResponse> adjustCurriculum(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TutorDTO.FeedbackChatRequest request
    ) {
        if (request.isFinished()) {
            // [수정] user.getUserEntity() -> user.userEntity()
            return ResponseEntity.ok(tutorService.adjustCurriculum(user.userEntity().getId(), request.planId(), request.message()));
        }
        return ResponseEntity.ok(new TutorDTO.FeedbackChatResponse("피드백을 입력해주세요.", null));
    }
}