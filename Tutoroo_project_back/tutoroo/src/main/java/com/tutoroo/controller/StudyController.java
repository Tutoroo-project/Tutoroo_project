package com.tutoroo.controller;

import com.tutoroo.dto.AssessmentDTO;
import com.tutoroo.dto.StudyDTO;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.AssessmentService;
import com.tutoroo.service.StudyService;
import com.tutoroo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
@Tag(name = "Study Management", description = "학습 관리 및 로드맵 API")
public class StudyController {

    private final StudyService studyService;
    private final UserService userService;
    private final AssessmentService assessmentService;

    @GetMapping("/status")
    @Operation(summary = "현재 학습 상태 조회", description = "메인 화면에서 현재 진행 중인 플랜의 상태와 진도율을 조회합니다.")
    public ResponseEntity<StudyDTO.StudyStatusResponse> getStudyStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Long planId // [New] 선택사항
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        // Service 메서드에 planId 전달
        return ResponseEntity.ok(studyService.getCurrentStudyStatus(user.getId(), planId));
    }

    /**
     * [수정됨] AssessmentDTO.RoadmapRequest를 직접 사용하여 불필요한 변환 제거
     * - 간편 생성 시에도 currentLevel(선택 사항)을 받을 수 있도록 확장성 확보
     */
    @PostMapping("/plans")
    @Operation(summary = "학습 플랜 간편 생성", description = "상담 없이 입력된 정보(목표, 선생님, 레벨 등)만으로 빠르게 로드맵을 생성합니다.")
    public ResponseEntity<AssessmentDTO.RoadmapResponse> createStudyPlan(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody AssessmentDTO.RoadmapRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        // DTO 변환 없이 바로 서비스로 전달 (깔끔한 처리)
        return ResponseEntity.ok(assessmentService.createStudentRoadmap(user.getId(), request));
    }

    @GetMapping("/plans/{planId}")
    @Operation(summary = "특정 플랜 상세 조회", description = "선택한 학습 플랜의 상세/로드맵 정보를 조회합니다.")
    public ResponseEntity<StudyDTO.PlanDetailResponse> getPlanDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getPlanDetail(user.getId(), planId));
    }


    @DeleteMapping("/plans/{planId}")
    @Operation(summary = "학습 플랜 삭제", description = "진행 중인 학습 플랜을 삭제합니다.")
    public ResponseEntity<Void> deleteStudyPlan(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        studyService.deleteStudyPlan(user.getId(), planId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logs")
    @Operation(summary = "학습 로그 저장", description = "오늘의 학습 내용이나 점수를 수동으로 기록합니다.")
    public ResponseEntity<String> saveStudyLog(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyDTO.StudyLogRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        studyService.saveSimpleLog(user.getId(), request);
        return ResponseEntity.ok("학습 기록이 저장되었습니다.");
    }

    @PostMapping("/chat/simple") // 명확성을 위해 URL을 /chat/simple로 구체화 (기존 /chat도 호환 가능)
    @Operation(summary = "학습 관련 간단 채팅", description = "현재 학습 중인 내용에 대해 AI 튜터에게 질문합니다.")
    public ResponseEntity<StudyDTO.ChatResponse> sendChatMessage(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyDTO.SimpleChatRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.handleSimpleChat(user.getId(), request.message()));
    }

    @GetMapping("/list")
    @Operation(summary = "내 학습 목록 조회", description = "진행 중이거나 완료된 모든 학습 플랜 목록을 조회합니다.")
    public ResponseEntity<List<StudyDTO.StudySimpleInfo>> getStudyList(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getActiveStudyList(user.getId()));
    }

    @GetMapping("/current")
    @Operation(summary = "현재 플랜 상세 조회", description = "대시보드에 표시할 현재 활성 플랜의 상세 정보를 가져옵니다.")
    public ResponseEntity<StudyDTO.PlanDetailResponse> getCurrentPlan(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getCurrentPlanDetail(user.getId()));
    }

    @GetMapping("/check-limit/{userId}")
    @Operation(summary = "플랜 생성 가능 여부 확인", description = "멤버십 등급에 따라 추가 플랜 생성이 가능한지 확인합니다.")
    public ResponseEntity<Boolean> checkLimit(@PathVariable Long userId) {
        return ResponseEntity.ok(studyService.canCreateNewGoal(userId));
    }

    @PatchMapping("/progress/{planId}")
    @Operation(summary = "진도율 강제 업데이트 (테스트용)", description = "특정 플랜의 진도율을 수정합니다.")
    public ResponseEntity<String> updateProgress(@PathVariable Long planId, @RequestParam double rate) {
        studyService.updateProgress(planId, (int) rate);
        return ResponseEntity.ok("진도율 업데이트 완료");
    }

    @GetMapping("/calendar")
    @Operation(summary = "월간 학습 캘린더 조회", description = "특정 연/월의 학습 기록(출석, 점수 등)을 조회합니다.")
    public ResponseEntity<StudyDTO.CalendarResponse> getMonthlyCalendar(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam int year,
            @RequestParam int month
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getMonthlyCalendar(user.getId(), year, month));
    }

    @PostMapping("/plans/{planId}/ai-feedback")
    public ResponseEntity<String> generateAiFeedback(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId
    ) {
        Long userId = user.getId();
        studyService.getPlanDetail(userId, planId);

        String feedback = studyService.generateAiFeedbackByPlanId(planId);
        userService.evictDashboardCache(user.getUsername());
        return ResponseEntity.ok(feedback);
    }
}