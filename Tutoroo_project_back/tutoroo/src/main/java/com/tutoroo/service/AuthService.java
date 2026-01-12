package com.tutoroo.service;

import com.tutoroo.dto.AuthDTO;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        UserEntity user = userMapper.findByUsername(request.getUsername());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new TutorooException(ErrorCode.INVALID_PASSWORD);
        }

        String token = jwtTokenProvider.createToken(user.getUsername(), user.getRole());

        return AuthDTO.LoginResponse.builder()
                .accessToken(token)
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void register(AuthDTO.JoinRequest request) {
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new TutorooException(ErrorCode.DUPLICATE_ID);
        }

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .age(request.getAge())
                .gender(request.getGender())
                .role("ROLE_USER")
                .totalPoint(0)
                .build();

        userMapper.save(user);
    }

    /**
     * [기능: 소셜 로그인 추가 정보 입력 처리]
     */
    @Transactional
    public AuthDTO.LoginResponse completeSocialSignup(String username, AuthDTO.SocialSignupRequest request, MultipartFile imageFile) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        // 1. 필수 정보 업데이트
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAge(request.getAge());
        user.setGender(request.getGender());

        // 2. 프로필 이미지 처리 (선택사항)
        if (imageFile != null && !imageFile.isEmpty()) {
            // 실제 S3 등의 저장 로직 대신 파일명만 저장 (예시)
            String imageUrl = "/uploads/" + imageFile.getOriginalFilename();
            user.setProfileImage(imageUrl);
        }

        // 3. 권한 승격 (GUEST -> USER)
        user.setRole("ROLE_USER");
        userMapper.updateSocialUser(user);

        // 4. 새로운 정식 토큰 발급 (ROLE_USER가 담긴 토큰)
        String newAccessToken = jwtTokenProvider.createToken(user.getUsername(), "ROLE_USER");

        return AuthDTO.LoginResponse.builder()
                .accessToken(newAccessToken)
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

        return new AuthDTO.AccountInfoResponse(user.getUsername(), "사용자의 아이디를 찾았습니다.", true);
    }

    @Transactional
    public void resetPassword(AuthDTO.ResetPasswordRequest request) {
        UserEntity user = userMapper.findByUsernameAndEmail(request.username(), request.email());

        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        userMapper.updatePassword(user.getId(), encodedPassword);
    }
}