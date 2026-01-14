package com.tutoroo.controller;

import com.tutoroo.dto.DashboardDTO;
import com.tutoroo.dto.UserDTO;
import com.tutoroo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * [기능: 대시보드 조회]
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.getAdvancedDashboard(username));
    }

    /**
     * [기능: 회원정보 통합 수정]
     * 설명: 비밀번호 인증을 거쳐 아이디, 비밀번호, 이메일, 전화번호를 수정하고
     * 프로필 이미지는 자유롭게 변경합니다.
     * 요청: Multipart/form-data
     * - data: JSON (currentPassword, newUsername, newPassword, email, phone)
     * - image: File (선택 사항)
     */
    @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateUserInfo(
            @RequestPart(value = "data") UserDTO.UpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        // 현재 로그인된 사용자의 ID(username) 추출
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // 서비스 호출
        userService.updateUserInfo(currentUsername, request, image);

        return ResponseEntity.ok("회원 정보가 성공적으로 변경되었습니다.");
    }
}