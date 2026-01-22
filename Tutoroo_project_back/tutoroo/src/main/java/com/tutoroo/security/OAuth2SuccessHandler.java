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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1. OAuth2User 정보 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        log.info("OAuth2 Login Success. Attributes: {}", attributes);

        //  [수정] CustomOAuth2UserService가 반환하는 principal은 CustomUserDetails임
        //    -> DB에 저장된 username(provider_providerId)을 토큰 subject로 써야 JWT 인증이 안 깨짐
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal(); // ✅ [수정]
        String username = principal.getUsername(); // [수정] (예: naver_12345, kakao_67890)

        // 2. 이메일 추출 (제공자별 속성 차이 대응)  (※ UI에서 이메일 선입력용이라 그대로 둠)
        String email = null;
        if (attributes != null) { // (방어)
            if (attributes.containsKey("email")) {
                email = (String) attributes.get("email");
            } else if (attributes.containsKey("kakao_account")) {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null && kakaoAccount.containsKey("email")) {
                    email = (String) kakaoAccount.get("email");
                }
            } else if (attributes.containsKey("response")) { // Naver
                Map<String, Object> responseMap = (Map<String, Object>) attributes.get("response");
                if (responseMap != null && responseMap.containsKey("email")) {
                    email = (String) responseMap.get("email");
                }
            }
        }

        // [수정] 이메일이 없더라도(예: 카카오 이메일 미동의) 로그인 자체를 막을지 정책 선택 가능
        // 지금은 기존 로직 유지하되, redirectUri 경로를 이상하게 붙이지 않도록만 최소 보정
        if (email == null) {
            log.error("Email not found in OAuth2 attributes");
            //  [수정] redirectUri는 보통 http://localhost:5173/oauth2/redirect 같은 페이지라 /login 붙이면 깨질 수 있음
            response.sendRedirect(UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "email_not_found")
                    .build().toUriString());
            return;
        }

        // 3. DB에서 유저 조회
        //  [수정] email로 찾지 말고, DB에 저장된 username(provider_providerId)로 찾아야 함
        UserEntity user = userMapper.findByUsername(username); // [수정]

        //  [수정] 권한을 하드코딩 ROLE_USER로 만들면 "추가정보 입력(ROLE_GUEST)" 흐름이 꼬일 수 있음
        String role = (user != null && user.getRole() != null) ? user.getRole() : "ROLE_GUEST"; // ✅ [수정]

        // 4. 토큰 생성을 위한 내부 Authentication 객체 생성
        // [수정] principal을 email이 아니라 username으로
        Authentication internalAuth = new UsernamePasswordAuthenticationToken(
                username, // [수정]
                null,
                Collections.singletonList(new SimpleGrantedAuthority(role)) // [수정]
        );

        // 5. 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(internalAuth);
        String refreshToken = jwtTokenProvider.generateRefreshToken(internalAuth);

        // 6. 프론트엔드로 리다이렉트
        // [수정] 신규 여부를 user==null 대신 ROLE_GUEST 기준으로 판단해야 모달이 뜨고(추가정보 입력 유도) 흐름이 일관됨
        boolean isNewUser = (user == null) || "ROLE_GUEST".equals(role); // [수정]

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("isNewUser", isNewUser) // [수정]
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}
