package com.tutoroo.security;

import com.tutoroo.jwt.JwtTokenProvider;
import com.tutoroo.security.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 현재 유저의 권한 확인
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        String accessToken = jwtTokenProvider.createToken(userDetails.getUsername(), role);

        String targetUrl;

        // [핵심 로직] GUEST면 추가 정보 입력 페이지, USER면 메인 페이지로 이동
        if ("ROLE_GUEST".equals(role)) {
            targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth/signup") // 프론트 회원가입 페이지
                    .queryParam("token", accessToken) // 임시 GUEST 토큰 전달
                    .queryParam("isNew", true)
                    .build().toUriString();
        } else {
            targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth/callback") // 로그인 완료 페이지
                    .queryParam("token", accessToken)
                    .build().toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}