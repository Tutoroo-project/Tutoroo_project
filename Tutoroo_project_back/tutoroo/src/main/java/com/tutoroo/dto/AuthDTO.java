package com.tutoroo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;


public class AuthDTO {

    // 1. 회원가입 요청
    @Builder
    public record JoinRequest(
            @NotBlank(message = "아이디는 필수입니다.")
            String username,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password,

            @NotBlank(message = "이름은 필수입니다.")
            String name,

            @NotBlank(message = "성별은 필수입니다.")
            String gender,

            @Min(value = 8, message = "8세 이상만 가입 가능합니다.")
            int age,

            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            String phone,

            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email,

            String parentPhone
    ) {}

    // 2. 로그인 요청
    @Builder
    public record LoginRequest(
            @NotBlank(message = "아이디를 입력해주세요.")
            String username,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            String password
    ) {}

    // 3. 로그인 응답
    @Builder
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String username,
            String name,
            String role,
            boolean isNewUser
    ) {}

    // 4. 소셜 회원가입 추가 정보 입력 요청
    @Builder
    public record SocialSignupRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            String email,

            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            String phone,

            @Min(value = 8, message = "8세 이상만 가입 가능합니다.")
            Integer age,

            @NotBlank(message = "성별은 필수입니다.")
            String gender,

            String parentPhone
    ) {}

    // 5. 아이디 찾기 요청
    @Builder
    public record FindIdRequest(
            String name,
            String email,
            String phone
    ) {}

    // 6. 비밀번호 초기화 요청
    @Builder
    public record ResetPasswordRequest(
            String username,
            String email,
            String newPassword
    ) {}

    // 7. 계정 정보 조회 공통 응답
    @Builder
    public record AccountInfoResponse(
            String result,
            String message,
            boolean success
    ) {}
}