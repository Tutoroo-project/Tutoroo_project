package com.tutoroo.controller;

import com.tutoroo.dto.PracticeDTO;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    // 1. 무한 실전 테스트 생성 (일반 모드 / 약점 모드)
    @PostMapping("/generate")
    public ResponseEntity<PracticeDTO.TestResponse> generateTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PracticeDTO.GenerateRequest request
    ) {
        return ResponseEntity.ok(practiceService.generatePracticeTest(user.getId(), request));
    }

    // 2. 테스트 제출 및 AI 채점
    @PostMapping("/submit")
    public ResponseEntity<PracticeDTO.GradingResponse> submitTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PracticeDTO.SubmitRequest request
    ) {
        return ResponseEntity.ok(practiceService.submitPracticeTest(user.getId(), request));
    }

    // 3. 오답 클리닉 (약점 분석) 페이지 데이터 조회
    @GetMapping("/weakness")
    public ResponseEntity<PracticeDTO.WeaknessAnalysisResponse> getWeakness(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId
    ) {
        return ResponseEntity.ok(practiceService.getWeaknessAnalysis(user.getId(), planId));
    }
}