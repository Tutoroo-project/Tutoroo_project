package com.tutoroo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [기능: 요청 로깅 및 성능 모니터링 필터]
 * 설명: 모든 API 요청의 URL, 처리 시간, 응답 상태 코드를 로깅하여 시스템 모니터링을 지원합니다.
 * 작동원리: 요청 시작 시각과 종료 시각을 계산하여 응답과 함께 로그를 기록합니다.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String method = request.getMethod();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // 상업적 운영 시 중요한 데이터: 성능 이슈가 있는 API를 즉각 파악 가능
            log.info("API Log - [{} {}] status: {} duration: {}ms", method, path, status, duration);
        }
    }
}