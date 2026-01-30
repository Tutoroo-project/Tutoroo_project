package com.tutoroo.config;

import com.tutoroo.filter.JwtAuthenticationFilter;
import com.tutoroo.jwt.JwtTokenProvider;
import com.tutoroo.security.OAuth2SuccessHandler;
import com.tutoroo.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
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
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 기본 보안 설정 비활성화 (Rest API 방식이므로 불필요)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // 2. CORS 설정 (프론트엔드 연동)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // 3. 세션 관리 (JWT 사용하므로 Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. URL별 접근 권한 관리 [핵심 수정 구간]
                .authorizeHttpRequests(auth -> auth
                        // [Everyone] 누구나 접근 가능 (로그인 불필요)
                        .requestMatchers(
                                "/", "/error", "/index.html",
                                "/api/auth/**",          // 로그인, 회원가입
                                "/api/public/**",        // 공지사항 등
                                "/api/payment/webhook",  // 결제 웹훅 (인증 없이 PG사 호출 허용)
                                "/swagger-ui/**", "/v3/api-docs/**" // API 문서
                        ).permitAll()

                        // [Static Resources] 이미지, 오디오 파일 접근 허용
                        .requestMatchers(
                                "/static/**",
                                "/images/**",
                                "/audio/**",
                                "/uploads/**",  // 업로드된 파일 접근 경로 추가
                                "/favicon.ico"
                        ).permitAll()

                        // [User Only] 로그인한 유저만 접근 가능 (여기에 practice 추가!)
                        .requestMatchers(
                                "/api/assessment/**",    // 진단 및 로드맵
                                "/api/tutor/**",         // AI 수업
                                "/api/study/**",         // 학습 관리
                                "/api/pet/**",           // 펫 관리
                                "/api/user/**",          // 마이페이지
                                "/api/notifications/**", // 알림 (누락분 추가)
                                "/api/payment/**",       // 결제 (누락분 추가)
                                "/api/ranking/**",       // 랭킹 (누락분 추가)
                                "/api/practice/**"       // [NEW] 실전 무한 테스트 (여기 추가되었습니다!)
                        ).hasAnyRole("USER", "ADMIN")

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 5. OAuth2 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // 6. JWT 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}