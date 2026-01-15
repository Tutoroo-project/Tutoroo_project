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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/join", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> register(
            @RequestPart("data") AuthDTO.JoinRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        authService.register(request, profileImage);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.LoginResponse> login(@RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // [신규] 토큰 재발급 엔드포인트
    @PostMapping("/reissue")
    public ResponseEntity<AuthDTO.LoginResponse> reissue(@RequestHeader("RefreshToken") String refreshToken) {
        return ResponseEntity.ok(authService.reissue(refreshToken));
    }

    @PostMapping(value = "/oauth/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthDTO.LoginResponse> completeSocialSignup(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("data") AuthDTO.SocialSignupRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
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

    @PostMapping("/find-password")
    public ResponseEntity<String> findPassword(@RequestBody AuthDTO.ResetPasswordRequest request) {
        authService.sendTemporaryPassword(request);
        return ResponseEntity.ok("가입된 이메일로 임시 비밀번호를 발송했습니다.");
    }

    @PostMapping("/email/send-verification")
    public ResponseEntity<String> sendEmailVerification(@RequestParam String email) {
        authService.requestEmailVerification(email);
        return ResponseEntity.ok("인증번호가 메일로 발송되었습니다.");
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Boolean> verifyEmail(@RequestParam String email, @RequestParam String code) {
        return ResponseEntity.ok(authService.verifyEmailCode(email, code));
    }
}