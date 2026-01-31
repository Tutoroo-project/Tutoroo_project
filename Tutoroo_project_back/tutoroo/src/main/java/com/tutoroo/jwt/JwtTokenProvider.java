package com.tutoroo.jwt;

import com.tutoroo.dto.TokenDto;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.security.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "Bearer";
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24;      // 1일
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7; // 7일

    private final Key key;
    private final CustomUserDetailsService customUserDetailsService;

    // 생성자 주입 (Secret Key & UserDetailsService)
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            CustomUserDetailsService customUserDetailsService) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * [1] 전체 토큰 생성 (Login 시 사용)
     * - Access + Refresh 토큰을 모두 생성하여 DTO로 반환
     */
    public TokenDto generateTokenDto(Authentication authentication) {
        // Access Token 생성
        String accessToken = generateAccessToken(authentication);
        // Refresh Token 생성
        String refreshToken = generateRefreshToken(authentication);
        // 만료 시간 계산 (DTO 반환용)
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

        return TokenDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * [1-1] Access Token 개별 생성 (UserService 등에서 사용)
     */
    public String generateAccessToken(Authentication authentication) {
        String authorities = getAuthorities(authentication);
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .setSubject(authentication.getName())       // payload "sub": "name"
                .claim(AUTHORITIES_KEY, authorities)        // payload "auth": "ROLE_USER"
                .setExpiration(accessTokenExpiresIn)        // payload "exp"
                .signWith(key, SignatureAlgorithm.HS512)    // header "alg": "HS512"
                .compact();
    }

    /**
     * [1-2] Refresh Token 개별 생성 (UserService 등에서 사용)
     */
    public String generateRefreshToken(Authentication authentication) {
        long now = (new Date()).getTime();
        Date refreshTokenExpiresIn = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .setSubject(authentication.getName()) // 필요시 subject에 username 저장
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * [2] 토큰에서 인증 정보 조회 (DB 조회 방식)
     * - 토큰에서 ID를 꺼낸 뒤, DB를 조회하여 최신 UserDetails를 가져옵니다.
     * - SecurityContext에 저장되어 Controller에서 @AuthenticationPrincipal로 사용됩니다.
     */
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new TutorooException("권한 정보가 없는 토큰입니다.", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // [중요] DB에서 진짜 유저 정보 조회 (CustomUserDetails)
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(claims.getSubject());

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * [3] 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    /**
     * [4] 토큰 남은 유효시간 조회
     * - 로그아웃 시 남은 시간만큼 Redis 블랙리스트에 저장하기 위해 필요
     */
    public Long getExpiration(String accessToken) {
        // accessToken 남은 유효시간
        Date expiration = parseClaims(accessToken).getExpiration();
        // 현재 시간
        Long now = new Date().getTime();
        return (expiration.getTime() - now);
    }

    /**
     * [5] 토큰에서 사용자 이름(Subject) 추출
     * - AuthService 로그아웃 로직 등에서 필요하여 추가
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // 내부적으로 토큰을 파싱하여 Claims(내용)를 반환
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // 내부 헬퍼: 권한 정보 문자열 추출
    private String getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}