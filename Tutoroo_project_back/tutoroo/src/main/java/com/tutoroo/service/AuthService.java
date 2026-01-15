package com.tutoroo.service;

import com.tutoroo.dto.AuthDTO;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.jwt.JwtTokenProvider;
import com.tutoroo.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final FileStore fileStore;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        UserEntity user = userMapper.findByUsername(request.username());

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new TutorooException(ErrorCode.INVALID_PASSWORD);
        }

        if ("WITHDRAWN".equals(user.getStatus())) {
            throw new TutorooException("탈퇴한 회원입니다.", ErrorCode.USER_NOT_FOUND);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());

        // Refresh Token을 Redis에 저장 (Key: "RT:{username}", Value: token)
        redisTemplate.opsForValue().set(
                "RT:" + user.getUsername(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpireTime(),
                TimeUnit.MILLISECONDS
        );

        return AuthDTO.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .isNewUser(false)
                .build();
    }

    public AuthDTO.LoginResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new TutorooException("Refresh Token이 유효하지 않습니다.", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String savedToken = redisTemplate.opsForValue().get("RT:" + username);

        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new TutorooException("Refresh Token 정보가 일치하지 않습니다.", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        UserEntity user = userMapper.findByUsername(username);
        String newAccessToken = jwtTokenProvider.createAccessToken(username, user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username);

        redisTemplate.opsForValue().set(
                "RT:" + username,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpireTime(),
                TimeUnit.MILLISECONDS
        );

        return AuthDTO.LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(username)
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void register(AuthDTO.JoinRequest request, MultipartFile profileImage) {
        if (userMapper.findByUsername(request.username()) != null) {
            throw new TutorooException(ErrorCode.DUPLICATE_ID);
        }

        if (request.age() < 19 && (request.parentPhone() == null || request.parentPhone().isBlank())) {
            throw new TutorooException(ErrorCode.PARENT_PHONE_REQUIRED);
        }

        String imagePath = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                imagePath = fileStore.storeFile(profileImage.getBytes(), ".png");
            } catch (Exception e) {
                log.error("프로필 이미지 저장 실패", e);
                throw new TutorooException("프로필 이미지 저장 중 오류 발생", ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        UserEntity user = UserEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .parentPhone(request.parentPhone())
                .age(request.age())
                .gender(request.gender())
                .role("ROLE_USER")
                .status("ACTIVE")
                .totalPoint(0)
                .profileImage(imagePath)
                .build();

        userMapper.save(user);
    }

    @Transactional
    public AuthDTO.LoginResponse completeSocialSignup(String tempUsername, AuthDTO.SocialSignupRequest request, MultipartFile profileImage) {
        UserEntity user = userMapper.findByUsername(tempUsername);
        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        if (request.age() < 19 && (request.parentPhone() == null || request.parentPhone().isBlank())) {
            throw new TutorooException(ErrorCode.PARENT_PHONE_REQUIRED);
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String path = fileStore.storeFile(profileImage.getBytes(), ".png");
                user.setProfileImage(path);
            } catch (Exception e) {
                log.warn("소셜 가입 프로필 이미지 저장 실패 (가입은 진행됨)", e);
            }
        }

        user.setPhone(request.phone());
        user.setParentPhone(request.parentPhone());
        user.setAge(request.age());
        user.setGender(request.gender());
        user.setRole("ROLE_USER");

        userMapper.updateSocialUser(user);

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUsername(), "ROLE_USER");
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());

        redisTemplate.opsForValue().set(
                "RT:" + user.getUsername(),
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpireTime(),
                TimeUnit.MILLISECONDS
        );

        return AuthDTO.LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(user.getUsername())
                .name(user.getName())
                .role("ROLE_USER")
                .isNewUser(false)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return userMapper.findByUsername(username) == null;
    }

    @Transactional(readOnly = true)
    public AuthDTO.AccountInfoResponse findUsername(AuthDTO.FindIdRequest request) {
        UserEntity user = userMapper.findByNameAndEmailAndPhone(
                request.name(), request.email(), request.phone());

        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        return new AuthDTO.AccountInfoResponse(user.getUsername(), "회원님의 아이디를 찾았습니다.", true);
    }

    @Transactional
    public void sendTemporaryPassword(AuthDTO.ResetPasswordRequest request) {
        UserEntity user = userMapper.findByUsernameAndEmail(request.username(), request.email());
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        String tempPw = UUID.randomUUID().toString().substring(0, 8);
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(tempPw));
        emailService.sendTemporaryPassword(user.getEmail(), tempPw);
    }

    public void requestEmailVerification(String email) {
        emailService.sendVerificationCode(email);
    }

    public boolean verifyEmailCode(String email, String code) {
        return emailService.verifyCode(email, code);
    }
}