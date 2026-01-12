package com.tutoroo.controller;

import com.tutoroo.dto.AssessmentDTO;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping("/consult")
    public ResponseEntity<AssessmentDTO.ConsultResponse> consult(
            @RequestBody AssessmentDTO.ConsultRequest request
    ) {
        return ResponseEntity.ok(assessmentService.consult(request));
    }

    @PostMapping("/roadmap")
    public ResponseEntity<AssessmentDTO.RoadmapResponse> generateRoadmap(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody AssessmentDTO.RoadmapRequest request
    ) {
        return ResponseEntity.ok(assessmentService.createStudentRoadmap(user.userEntity().getId(), request));
    }
}