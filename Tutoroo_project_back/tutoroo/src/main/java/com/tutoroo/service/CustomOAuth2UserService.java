package com.tutoroo.service;

import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.security.oauth.info.GoogleOAuth2UserInfo;
import com.tutoroo.security.oauth.info.KakaoOAuth2UserInfo;
import com.tutoroo.security.oauth.info.NaverOAuth2UserInfo;
import com.tutoroo.security.oauth.info.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = switch (registrationId) {
            case "google" -> new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
            case "kakao" -> new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
            case "naver" -> new NaverOAuth2UserInfo(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        };

        String username = userInfo.getProvider() + "_" + userInfo.getProviderId();
        UserEntity userEntity = userMapper.findByUsername(username);

        if (userEntity == null) {
            // [핵심] 최초 가입 시 ROLE_GUEST 부여 -> 추가 정보 입력 유도
            userEntity = UserEntity.builder()
                    .username(username)
                    .password(UUID.randomUUID().toString())
                    .name(userInfo.getName())
                    .email(userInfo.getEmail())
                    .profileImage(userInfo.getProfileImage())
                    .role("ROLE_GUEST") // GUEST 권한 설정
                    .provider(userInfo.getProvider())
                    .totalPoint(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            userMapper.save(userEntity);
        }

        return new CustomUserDetails(userEntity, oAuth2User.getAttributes());
    }
}