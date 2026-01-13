package com.tutoroo.security;

import com.tutoroo.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// [수정] OAuth2User 인터페이스 추가 구현
public record CustomUserDetails(
        UserEntity userEntity,
        Map<String, Object> attributes // OAuth2 제공자로부터 받은 원본 데이터
) implements UserDetails, OAuth2User {

    // 일반 로그인용 생성자
    public CustomUserDetails(UserEntity userEntity) {
        this(userEntity, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
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
        return userEntity.getName();
    }
    // -----------------------

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}