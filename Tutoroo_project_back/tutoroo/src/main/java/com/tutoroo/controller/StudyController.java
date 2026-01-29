package com.tutoroo.controller;

import com.tutoroo.dto.StudyDTO;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.security.CustomUserDetails;
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
@Tag(name = "Study Management", description = "학습 관리, AI 채팅 및 세션 API")
public class StudyController {

    private final StudyService studyService;
    private final UserService userService;

    // =================================================================================
    // 1. 조회 API (상태, 상세, 목록, 캘린더)
    // =================================================================================

    @GetMapping("/status")
    @Operation(summary = "현재 학습 상태 조회", description = "메인 화면 위젯용: 현재 진행 중인 플랜의 상태, 진도율, 오늘 학습 여부를 조회합니다.")
    public ResponseEntity<StudyDTO.StudyStatusResponse> getStudyStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Long planId
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getStudyStatus(user.getId(), planId));
    }

    @GetMapping("/plans/{planId}")
    @Operation(summary = "특정 플랜 상세 조회", description = "선택한 학습 플랜의 상세 정보와 로드맵을 조회합니다.")
    public ResponseEntity<StudyDTO.PlanDetailResponse> getPlanDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getPlanDetail(user.getId(), planId));
    }

    @GetMapping("/current")
    @Operation(summary = "현재 활성 플랜 상세 조회", description = "대시보드 메인에 표시할 가장 최근의 활성 플랜 정보를 가져옵니다.")
    public ResponseEntity<StudyDTO.PlanDetailResponse> getCurrentPlan(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getCurrentPlanDetail(user.getId()));
    }

    @GetMapping("/list")
    @Operation(summary = "내 학습 목록 조회", description = "사이드바 등에 표시할 내 모든 학습 플랜의 간략한 목록을 조회합니다.")
    public ResponseEntity<List<StudyDTO.StudySimpleInfo>> getStudyList(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getActiveStudyList(user.getId()));
    }

    @GetMapping("/calendar")
    @Operation(summary = "월간 학습 캘린더 조회", description = "특정 연/월의 출석 현황과 학습 기록을 조회합니다.")
    public ResponseEntity<StudyDTO.CalendarResponse> getMonthlyCalendar(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam int year,
            @RequestParam int month
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.getMonthlyCalendar(user.getId(), year, month));
    }

    // =================================================================================
    // 2. 관리 API (생성, 삭제, 로그, 진도율)
    // =================================================================================

    @PostMapping("/plans")
    @Operation(summary = "학습 플랜 생성", description = "새로운 학습 목표와 선생님 페르소나를 설정하여 플랜을 생성합니다.")
    public ResponseEntity<Long> createStudyPlan(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyDTO.CreatePlanRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.createPlan(user.getId(), request));
    }

    @DeleteMapping("/plans/{planId}")
    @Operation(summary = "학습 플랜 삭제", description = "진행 중인 학습 플랜을 삭제하고 관련 세션 데이터를 정리합니다.")
    public ResponseEntity<Void> deleteStudyPlan(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        studyService.deleteStudyPlan(user.getId(), planId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logs")
    @Operation(summary = "학습 로그 저장", description = "오늘의 학습 내용, 점수, 소감을 저장하고 포인트를 지급합니다.")
    public ResponseEntity<String> saveStudyLog(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyDTO.StudyLogRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        studyService.saveSimpleLog(user.getId(), request);
        return ResponseEntity.ok("학습 기록이 저장되었습니다.");
    }

    @PatchMapping("/progress/{planId}")
    @Operation(summary = "진도율 강제 업데이트", description = "특정 플랜의 진도율을 수동으로 수정합니다. (주로 테스트용)")
    public ResponseEntity<String> updateProgress(@PathVariable Long planId, @RequestParam int rate) {
        studyService.updateProgress(planId, rate);
        return ResponseEntity.ok("진도율 업데이트 완료");
    }

    @GetMapping("/check-limit/{userId}")
    @Operation(summary = "플랜 생성 가능 여부 확인", description = "멤버십 등급에 따라 추가 플랜 생성이 가능한지 확인합니다.")
    public ResponseEntity<Boolean> checkLimit(@PathVariable Long userId) {
        return ResponseEntity.ok(studyService.canCreateNewGoal(userId));
    }

    // =================================================================================
    // 3. AI 및 채팅 API
    // =================================================================================

    @PostMapping("/chat/simple")
    @Operation(summary = "학습 관련 AI 채팅", description = "로드맵 컨텍스트가 주입된 AI 선생님과 실시간으로 대화합니다.")
    public ResponseEntity<StudyDTO.ChatResponse> sendChatMessage(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyDTO.SimpleChatRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(studyService.handleSimpleChat(
                user.getId(),
                request.planId(),
                request.message()
        ));
    }

    @PostMapping("/plans/{planId}/ai-feedback")
    @Operation(summary = "AI 상세 피드백 생성", description = "최신 학습 로그를 바탕으로 AI 선생님의 상세한 피드백을 생성합니다.")
    public ResponseEntity<String> generateAiFeedback(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        // 권한 체크 겸 플랜 조회
        studyService.getPlanDetail(user.getId(), planId);

        String feedback = studyService.generateAiFeedbackByPlanId(planId);
        userService.evictDashboardCache(user.getUsername()); // 대시보드 갱신

        return ResponseEntity.ok(feedback);
    }

    // =================================================================================
    // 4. 세션 관리 API (New - 누락되었던 기능 복구)
    // =================================================================================

    @GetMapping("/plans/{planId}/session")
    @Operation(summary = "학습 세션 상태 조회 (이어하기)", description = "중단된 학습 세션(퀴즈 진행 상황 등)의 상태 데이터를 조회합니다.")
    public ResponseEntity<String> getSessionState(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        // 본인 플랜인지 검증
        studyService.getPlanDetail(user.getId(), planId);

        String sessionState = studyService.getSessionState(planId);
        return ResponseEntity.ok(sessionState != null ? sessionState : "{}");
    }

    @PostMapping("/plans/{planId}/session")
    @Operation(summary = "학습 세션 상태 저장", description = "현재 학습 진행 상황(JSON)을 Redis에 임시 저장합니다.")
    public ResponseEntity<Void> saveSessionState(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId,
            @RequestBody String stateJson
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        // 본인 플랜인지 검증
        studyService.getPlanDetail(user.getId(), planId);

        studyService.saveSessionState(planId, stateJson);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/plans/{planId}/session")
    @Operation(summary = "학습 세션 초기화", description = "학습이 완료되거나 취소되었을 때 임시 세션 데이터를 삭제합니다.")
    public ResponseEntity<Void> clearSessionState(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        studyService.clearSessionState(planId);
        return ResponseEntity.ok().build();
    }
}