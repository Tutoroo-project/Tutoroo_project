package com.tutoroo.controller;

import com.tutoroo.dto.DashboardDTO;
import com.tutoroo.dto.UserDTO;
import com.tutoroo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.getAdvancedDashboard(username));
    }

    @PatchMapping("/update")
    public ResponseEntity<String> updateUserInfo(@RequestBody UserDTO.UpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateUserInfo(username, request);
        return ResponseEntity.ok("사용자 정보가 성공적으로 수정되었습니다.");
    }
}