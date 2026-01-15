package com.tutoroo.security;

import com.tutoroo.entity.UserEntity;
import com.tutoroo.jwt.JwtTokenProvider;
import com.tutoroo.mapper.UserMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate; // [추가] 리프레시 토큰 저장용

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // CustomOAuth2UserService에서 loadUser()가 반환한 OAuth2User의 식별자(username)를 가져옵니다.
        String username = authentication.getName();

        // DB에서 사용자 조회
        UserEntity user = userMapper.findByUsername(username);

        String targetUrl;

        // 1. 신규 회원이거나 추가 정보가 필요한 경우 (ROLE_GUEST) -> 프론트엔드 회원가입 페이지로 리다이렉트
        if (user == null || "ROLE_GUEST".equals(user.getRole())) {
            targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/signup/social") // 프론트엔드 주소 (가정)
                    .queryParam("username", username)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
        }
        // 2. 기존 회원인 경우 (ROLE_USER) -> 로그인 처리 (토큰 발급 후 메인으로)
        else {
            // [수정] 변경된 메서드명 사용 (createToken -> createAccessToken)
            String accessToken = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole());

            // [추가] 리프레시 토큰 생성
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());

            // [추가] 리프레시 토큰 Redis 저장 (유효기간: 14일)
            redisTemplate.opsForValue().set(
                    "RT:" + user.getUsername(),
                    refreshToken,
                    jwtTokenProvider.getRefreshTokenExpireTime(),
                    TimeUnit.MILLISECONDS
            );

            // 프론트엔드로 토큰 전달 (로그인 콜백 페이지)
            targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
        }

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}