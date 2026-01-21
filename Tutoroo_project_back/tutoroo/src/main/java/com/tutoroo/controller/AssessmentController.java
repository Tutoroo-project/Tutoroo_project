package com.tutoroo.controller;

import com.tutoroo.dto.AssessmentDTO;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
@Tag(name = "Assessment", description = "AI 레벨 테스트 및 심층 상담 API")
public class AssessmentController {

    private final AssessmentService assessmentService;

    /**
     * [Step 2] 수준 파악 심층 상담 진행
     * - AI가 10턴 이상의 대화를 유도하거나, 유저 의도에 따라 조기 종료합니다.
     * - 프론트엔드는 응답의 isFinished=true가 될 때까지 이 API를 반복 호출해야 합니다.
     */
    @PostMapping("/consult")
    @Operation(summary = "심층 상담 진행", description = "AI와 대화를 주고받으며 수준을 파악합니다. isFinished=true가 될 때까지 반복 호출하세요.")
    public ResponseEntity<AssessmentDTO.ConsultResponse> consult(
            @RequestBody AssessmentDTO.ConsultRequest request
    ) {
        return ResponseEntity.ok(assessmentService.proceedConsultation(request));
    }

    /**
     * [Step 3] 상담 종료 후 최종 로드맵 생성
     * - 상담 내역(History)을 바탕으로 AI가 상세 커리큘럼(목차+상세)을 생성하고 DB에 저장합니다.
     * - 엔드포인트 명 변경: /generate -> /submit (제출 및 생성의 의미 강화)
     */
    @PostMapping("/submit")
    @Operation(summary = "상담 결과 제출 및 로드맵 생성", description = "상담이 종료되면 호출하여 최종 학습 로드맵을 생성합니다.")
    public ResponseEntity<AssessmentDTO.AssessmentResultResponse> submitAssessment(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody AssessmentDTO.AssessmentSubmitRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(assessmentService.analyzeAndCreateRoadmap(user.getId(), request));
    }

    // --- 기타 기능 (레벨 테스트 및 로드맵 재생성) ---

    @PostMapping("/test/start")
    @Operation(summary = "간편 레벨 테스트 시작", description = "5지선다형 테스트 문제를 생성합니다.")
    public ResponseEntity<AssessmentDTO.LevelTestResponse> startLevelTest(
            @RequestBody AssessmentDTO.LevelTestRequest request
    ) {
        return ResponseEntity.ok(assessmentService.generateLevelTest(request));
    }

    @PostMapping("/test/submit")
    @Operation(summary = "레벨 테스트 제출", description = "테스트 답안을 채점하고 결과를 반환합니다.")
    public ResponseEntity<AssessmentDTO.AssessmentResult> submitLevelTest(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody AssessmentDTO.TestSubmitRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(assessmentService.evaluateLevelTest(user.getId(), request));
    }

    @PostMapping("/roadmap/regenerate")
    @Operation(summary = "로드맵 재생성", description = "기존 플랜이 마음에 들지 않을 경우 상담 내용을 바탕으로 다시 생성합니다.")
    public ResponseEntity<AssessmentDTO.AssessmentResultResponse> regenerateRoadmap(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,
            @RequestBody AssessmentDTO.AssessmentSubmitRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(assessmentService.regenerateRoadmap(user.getId(), planId, request));
    }
}