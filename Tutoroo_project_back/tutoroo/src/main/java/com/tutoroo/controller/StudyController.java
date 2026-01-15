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

    @GetMapping("/check-limit/{userId}")
    public ResponseEntity<Boolean> checkLimit(@PathVariable Long userId) {
        return ResponseEntity.ok(studyService.canCreateNewGoal(userId));
    }

    @PatchMapping("/progress/{planId}")
    public ResponseEntity<String> updateProgress(
            @PathVariable Long planId,
            @RequestParam double rate
    ) {
        studyService.updateProgress(planId, (int) rate);
        return ResponseEntity.ok("진도율이 성공적으로 반영되었습니다.");
    }
}