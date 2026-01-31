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

                // 4. URL별 접근 권한 관리 [핵심 수정]
                .authorizeHttpRequests(auth -> auth
                        // [Everyone] 누구나 접근 가능
                        .requestMatchers(
                                "/", "/error", "/index.html",
                                "/api/auth/**",          // 로그인, 회원가입, 토큰재발급
                                "/api/public/**",        // 공지사항 등
                                "/api/payment/webhook",  // 결제 웹훅 (인증 없이 호출됨)
                                "/swagger-ui/**", "/v3/api-docs/**", // API 문서
                                "/uploads/**"            // 업로드된 파일 접근 허용
                        ).permitAll()

                        // [Static Resources] 정적 파일 접근 허용 (이미지, 오디오)
                        .requestMatchers(
                                "/static/**",
                                "/images/**",   // AI 생성 이미지 경로
                                "/audio/**",    // TTS 생성 오디오 경로
                                "/favicon.ico"
                        ).permitAll()

                        // [User Only] 학생 전용 기능 (로그인 필수)
                        .requestMatchers(
                                "/api/assessment/**",    // 진단 상담 및 로드맵
                                "/api/tutor/**",         // AI 수업 및 시험
                                "/api/study/**",         // 학습 대시보드
                                "/api/pet/**",           // 펫 키우기
                                "/api/user/**",          // 내 정보 수정
                                "/api/notifications/**", // 알림 확인
                                "/api/payment/**",       // 결제 요청/내역
                                "/api/ranking/**",       // 랭킹 조회
                                "/api/practice/**"       // [NEW] 실전 무한 테스트 (여기 추가됨!)
                        ).hasAnyRole("USER", "ADMIN")

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 5. OAuth2 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // 6. JWT 필터 등록 (UsernamePasswordFilter 앞에서 동작)
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