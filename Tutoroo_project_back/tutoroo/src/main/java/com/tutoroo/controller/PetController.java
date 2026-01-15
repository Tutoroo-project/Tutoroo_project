package com.tutoroo.controller;

import com.tutoroo.dto.PetDTO;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    // 펫 상태 조회 (메인 화면 등에서 주기적으로 호출)
    @GetMapping("/status")
    public ResponseEntity<PetDTO.PetStatusResponse> getStatus(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(petService.getPetStatus(user.getId()));
    }

    // 밥주기 / 놀아주기
    @PostMapping("/interact")
    public ResponseEntity<PetDTO.PetStatusResponse> interact(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PetDTO.InteractionRequest request) {
        return ResponseEntity.ok(petService.interact(user.getId(), request.actionType()));
    }
}