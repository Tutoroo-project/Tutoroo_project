package com.tutoroo.security;

import com.tutoroo.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record CustomUserDetails(
        UserEntity userEntity,
        Map<String, Object> attributes // OAuth2 제공자로부터 받은 원본 데이터 (일반 로그인 시 null)
) implements UserDetails, OAuth2User {

    // 일반 로그인용 생성자 (OAuth2 속성 없음)
    public CustomUserDetails(UserEntity userEntity) {
        this(userEntity, null);
    }

    // [핵심] 컨트롤러에서 user.getId()를 호출하기 위한 편의 메서드
    public Long getId() {
        return userEntity.getId();
    }

    // [편의] 멤버십 등급 바로 꺼내기
    public String getTier() {
        return userEntity.getEffectiveTier().name();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 권한이 없으면 빈 리스트 반환, 있으면 해당 권한 반환
        if (userEntity.getRole() == null) {
            return Collections.emptyList();
        }
        return List.of(new SimpleGrantedAuthority(userEntity.getRole()));
    }

    @Override
    public String getPassword() {
        return userEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return userEntity.getUsername();
    }

    // --- OAuth2User 구현 ---
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return userEntity.getUsername(); // OAuth2User의 식별자로 username 사용
    }
    // -----------------------

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // [보안] 탈퇴(WITHDRAWN)하거나 정지(BANNED)된 회원은 false 반환 -> 로그인 차단됨
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(userEntity.getStatus());
    }
}