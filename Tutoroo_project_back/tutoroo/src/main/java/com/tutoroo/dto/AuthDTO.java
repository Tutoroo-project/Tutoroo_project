package com.tutoroo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * [기능: 인증 데이터 전송 객체]
 * 설명: 로그인, 회원가입, 소셜 추가 정보 입력을 위한 DTO입니다.
 */
public class AuthDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        private String username;
        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
        private String name;
        private String gender;
        private int age;
        private String phone;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        private String username;
        @NotBlank(message = "비밀번호를 입력해주세요.")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private String username;
        private String name;
        private String role;
        private boolean isNewUser; // 소셜 최초 접속 여부
    }

    /**
     * [기능: 소셜 회원가입 추가 정보 입력 요청]
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialSignupRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        private String phone;

        @NotNull(message = "나이는 필수입니다.")
        @Min(value = 8, message = "8세 이상만 가입 가능합니다.")
        private Integer age;

        @NotBlank(message = "성별은 필수입니다.")
        private String gender; // MALE, FEMALE
    }

    /** [아이디 찾기 요청] */
    public record FindIdRequest(
            String name,
            String email,
            String phone
    ) {}

    /** [비밀번호 찾기/재설정 요청] */
    public record ResetPasswordRequest(
            String username,
            String email,
            String newPassword
    ) {}

    /** [계정 정보 응답] */
    public record AccountInfoResponse(
            String username,
            String message,
            boolean success
    ) {}
}