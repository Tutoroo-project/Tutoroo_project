package com.tutoroo.filter;

import com.tutoroo.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request에서 토큰 추출 (헤더 우선, 특정 API는 파라미터 허용)
        String token = resolveToken(request);

        // 2. validateToken으로 토큰 유효성 검사
        if (token != null && jwtTokenProvider.validateToken(token)) {

            // 3. Redis에 해당 토큰이 블랙리스트(로그아웃)로 등록되어 있는지 확인
            // 키 형식: "BL:" + AccessToken
            String isLogout = redisTemplate.opsForValue().get("BL:" + token);

            if (isLogout != null) {
                // 로그아웃된 토큰인 경우 경고 로그만 남기고 인증 객체는 저장하지 않음 (결국 401/403 발생)
                log.warn("🚨 로그아웃된 토큰으로 접근이 감지되었습니다. URI: {}", request.getRequestURI());
            } else {
                // 4. 토큰이 유효하고 블랙리스트에 없다면 토큰에서 인증 정보(Authentication) 가져오기
                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // 5. SecurityContext에 Authentication 객체 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("✅ Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", authentication.getName(), request.getRequestURI());
            }
        }

        // 6. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * [수정됨] 토큰 추출 로직 개선
     * 1. 기본: Authorization 헤더에서 Bearer 토큰 추출
     * 2. 예외: 알림 구독(SSE) 요청은 URL 쿼리 파라미터(?token=...)에서 추출
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. 헤더에서 추출 (표준 방식)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. [추가] SSE 연결 요청(/api/notifications/subscribe)인 경우 쿼리 파라미터 허용
        // 이유: JavaScript EventSource는 헤더 설정이 불가능하므로 ?token=... 방식을 허용해야 함
        if (request.getRequestURI().startsWith("/api/notifications/subscribe")) {
            String queryToken = request.getParameter("token");
            if (StringUtils.hasText(queryToken)) {
                return queryToken;
            }
        }

        return null;
    }
}