package com.tutoroo.controller;

import com.tutoroo.dto.AuthDTO;
import com.tutoroo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * [기능: 인증 및 계정 관리 통합 컨트롤러]
 * 설명: 로그인, 가입, 중복체크, 소셜 추가정보 입력, 계정 찾기를 모두 포함합니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.LoginResponse> login(@RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/join")
    public ResponseEntity<String> register(@RequestBody AuthDTO.JoinRequest request) {
        authService.register(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    /**
     * [기능: 소셜 회원가입 추가 정보 입력]
     * 조건: ROLE_GUEST 토큰을 헤더에 담아 요청해야 함
     */
    @PostMapping(value = "/oauth/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthDTO.LoginResponse> completeSocialSignup(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("data") AuthDTO.SocialSignupRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        // userDetails.getUsername()에는 "kakao_12345" 같은 ID가 들어있음
        return ResponseEntity.ok(authService.completeSocialSignup(userDetails.getUsername(), request, profileImage));
    }

    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkId(@RequestParam String username) {
        return ResponseEntity.ok(authService.isUsernameAvailable(username));
    }

    @PostMapping("/find-id")
    public ResponseEntity<AuthDTO.AccountInfoResponse> findId(@RequestBody AuthDTO.FindIdRequest request) {
        return ResponseEntity.ok(authService.findUsername(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody AuthDTO.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }
}