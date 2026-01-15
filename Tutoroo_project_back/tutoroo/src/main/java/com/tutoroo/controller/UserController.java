package com.tutoroo.controller;

import com.tutoroo.dto.DashboardDTO;
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

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getAdvancedDashboard(user.getUsername()));
    }

    @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateUserInfo(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart(value = "data") UserDTO.UpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        userService.updateUserInfo(user.getUsername(), request, image);
        return ResponseEntity.ok("회원 정보가 성공적으로 변경되었습니다.");
    }
}