package com.tutoroo.config;

import com.tutoroo.filter.JwtAuthenticationFilter;
import com.tutoroo.filter.RequestLoggingFilter;
import com.tutoroo.jwt.JwtTokenProvider;
import com.tutoroo.security.CustomUserDetailsService;
import com.tutoroo.security.OAuth2SuccessHandler;
import com.tutoroo.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final RequestLoggingFilter requestLoggingFilter;

    // [핵심 해결] 비밀번호 암호화 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // [추가] 인증 관리자 빈 등록 (Spring Security 설정 표준)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 세션 사용 안 함 (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 요청 권한 관리
                .authorizeHttpRequests(auth -> auth
                        // [공개] 정적 리소스 (오디오 파일, 이미지 등)
                        .requestMatchers("/audio/**", "/images/**", "/css/**", "/js/**").permitAll()

                        // [공개] Swagger 문서
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // [공개] 인증 관련 (회원가입, 로그인, 아이디 찾기 등)
                        .requestMatchers("/api/auth/**").permitAll()

                        // [공개] 랭킹 조회 (비회원도 볼 수 있음)
                        .requestMatchers("/api/ranking/realtime", "/api/ranking/list").permitAll()

                        // [인증 필요] 그 외 모든 API (결제, 펫, 알림, 튜터링 등)
                        .anyRequest().authenticated()
                )

                // 5. JWT 필터 및 로깅 필터 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestLoggingFilter, JwtAuthenticationFilter.class)

                // 6. OAuth2 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 프론트엔드 도메인
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));

        // 허용할 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 자격 증명(쿠키, 인증 헤더) 허용
        configuration.setAllowCredentials(true);

        // 클라이언트가 읽을 수 있는 헤더 노출 (토큰)
        configuration.setExposedHeaders(List.of("Authorization", "RefreshToken"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}