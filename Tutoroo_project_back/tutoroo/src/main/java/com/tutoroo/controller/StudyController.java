package com.tutoroo.controller;

import com.tutoroo.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    /**
     * [기능: 추가 목표 설정 가능 여부 체크]
     * - 등급(BASIC, STANDARD)에 따라 목표 생성 제한 확인
     */
    @GetMapping("/check-limit/{userId}")
    public ResponseEntity<Boolean> checkLimit(@PathVariable Long userId) {
        return ResponseEntity.ok(studyService.canCreateNewGoal(userId));
    }

    /**
     * [기능: 수업 종료 후 진도율 갱신]
     * - 오류 해결: @RequestParam으로 받은 double을 (int)로 캐스팅하여 전달
     */
    @PatchMapping("/progress/{planId}")
    public ResponseEntity<String> updateProgress(
            @PathVariable Long planId,
            @RequestParam double rate // 클라이언트에서 50.5 같이 보낼 수 있음
    ) {
        // [수정 포인트] (int) rate -> Service는 Integer를 요구하므로 형변환 필수
        studyService.updateProgress(planId, (int) rate);

        return ResponseEntity.ok("진도율이 성공적으로 반영되었습니다.");
    }
}