package com.tutoroo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenDto {
    private String grantType;     // "Bearer"
    private String accessToken;   // 액세스 토큰
    private String refreshToken;  // 리프레시 토큰
    private Long accessTokenExpiresIn; // 만료 시간
}