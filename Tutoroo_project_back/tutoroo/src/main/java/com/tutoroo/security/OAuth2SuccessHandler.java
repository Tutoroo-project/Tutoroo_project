package com.tutoroo.security;

import com.tutoroo.entity.UserEntity;
import com.tutoroo.jwt.JwtTokenProvider;
import com.tutoroo.mapper.UserMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // 1) OAuth2User (실제론 CustomUserDetails가 들어옴)
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info("OAuth2 Login Success. Attributes: {}", oAuth2User.getAttributes());

        //  [수정 1] username을 이메일이 아니라 "provider_providerId"로 사용
        // CustomOAuth2UserService에서 이미 이렇게 만들어 DB에 저장함.
        String username;
        UserEntity user;

        if (oAuth2User instanceof CustomUserDetails cud) {
            username = cud.getUsername();
            user = cud.userEntity(); // 이미 DB 유저 엔티티가 들어있음
        } else {
            // 예외 케이스 대비: 그래도 DB username으로 조회해야 함
            // (원래 로직처럼 email로 findByUsername 하면 계속 null 나옴)
            String fallbackUsername = oAuth2User.getName();
            username = fallbackUsername;
            user = userMapper.findByUsername(username);
        }

        //  [수정 2] isNewUser 판단을 (user==null) 말고 ROLE_GUEST 여부로 판단
        boolean isNewUser = (user != null && "ROLE_GUEST".equals(user.getRole()));

        // 2) 토큰 생성용 Authentication도 username 기준으로 생성해야 함
        Authentication internalAuth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                authentication.getAuthorities()
        );

        // 3) 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(internalAuth);
        String refreshToken = jwtTokenProvider.generateRefreshToken(internalAuth);

        // 4) 프론트로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("isNewUser", isNewUser)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}
