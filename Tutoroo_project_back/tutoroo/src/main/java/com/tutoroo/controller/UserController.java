package com.tutoroo.controller;

import com.tutoroo.dto.UserDTO;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. 대시보드 조회
    @GetMapping("/dashboard")
    public ResponseEntity<UserDTO.DashboardDTO> getDashboard(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getAdvancedDashboard(user.getUsername()));
    }

    // 2. 회원 정보 수정 (이미지 포함)
    @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateUserInfo(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart(value = "data") UserDTO.UpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        userService.updateUserInfo(user.getUsername(), request, image);
        return ResponseEntity.ok("회원 정보가 성공적으로 변경되었습니다.");
    }

    // 3. 라이벌 매칭 요청
    @PostMapping("/match-rival")
    public ResponseEntity<String> matchRival(@AuthenticationPrincipal CustomUserDetails user) {
        String result = userService.matchRival(user.getId());
        return ResponseEntity.ok(result);
    }

    // 4. 회원 탈퇴 요청
    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UserDTO.WithdrawRequest request) {

        userService.withdrawUser(user.getId(), request);
        return ResponseEntity.ok("회원 탈퇴 처리가 완료되었습니다. 90일 후 데이터가 영구 삭제됩니다.");
    }
}