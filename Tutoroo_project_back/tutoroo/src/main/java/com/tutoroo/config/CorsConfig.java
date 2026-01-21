package com.tutoroo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    // [수정] application.yml에서 허용할 도메인 리스트를 주입받음
    // 값이 없으면 기본값으로 로컬호스트(5173) 설정
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 1. 허용할 프론트엔드 도메인 (설정 파일 값 사용)
        config.setAllowedOrigins(allowedOrigins);

        // 2. 허용할 HTTP 메서드
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 3. 허용할 헤더
        config.setAllowedHeaders(List.of("*"));

        // 4. 자격 증명 허용 (쿠키, Authorization 헤더 등)
        config.setAllowCredentials(true);

        // 5. 노출할 헤더 (프론트에서 엑세스 토큰 접근 허용 등)
        // [보완] Content-Disposition 추가 (파일 다운로드 시 파일명 확인용)
        config.setExposedHeaders(List.of("Authorization", "RefreshToken", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}