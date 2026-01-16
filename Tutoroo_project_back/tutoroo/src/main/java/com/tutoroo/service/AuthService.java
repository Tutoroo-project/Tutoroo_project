package com.tutoroo.service;

import com.tutoroo.dto.AuthDTO;
import com.tutoroo.entity.MembershipTier;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.jwt.JwtTokenProvider;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
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
    private final FileStore fileStore; // [필수] 파일 저장소
    private final RedisTemplate<String, String> redisTemplate;

    // --- [1] 로그인 ---
    @Transactional(readOnly = true)
    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        UserEntity user = userMapper.findByUsername(request.username());

        // 1. 유저 존재 여부 및 비밀번호 확인
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new TutorooException(ErrorCode.INVALID_PASSWORD);
        }

        // 2. 계정 상태 확인
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new TutorooException("비활성화된 계정입니다. 관리자에게 문의하세요.", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 3. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());

        // 4. 리프레시 토큰 Redis 저장 (유효기간 14일)
        redisTemplate.opsForValue().set(
                "RT:" + user.getUsername(),
                refreshToken,
                14,
                TimeUnit.DAYS
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

    // --- [2] 일반 회원가입 ---
    @Transactional
    public void register(AuthDTO.JoinRequest request, MultipartFile profileImage) {
        // 1. 중복 확인
        if (userMapper.findByUsername(request.username()) != null) {
            throw new TutorooException(ErrorCode.DUPLICATE_ID);
        }

        // 2. 프로필 이미지 저장 (FileStore 사용)
        String profileImageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            // "users" 라는 접두사를 붙여서 확장자 처리 등은 FileStore에 위임
            // 여기서는 단순하게 byte[]를 넘김
            try {
                // 원본 파일명에서 확장자 추출
                String originalFilename = profileImage.getOriginalFilename();
                String ext = (originalFilename != null && originalFilename.contains("."))
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".jpg"; // 기본값

                profileImageUrl = fileStore.storeFile(profileImage.getBytes(), ext);
            } catch (Exception e) {
                log.error("프로필 이미지 저장 실패: {}", e.getMessage());
                // 이미지는 선택사항이므로 실패해도 가입은 진행하되 경고 로그 남김 (혹은 에러 던지기 선택)
            }
        }

        // 3. 엔티티 생성
        UserEntity newUser = UserEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .gender(request.gender())
                .age(request.age())
                .phone(request.phone())
                .email(request.email())
                .parentPhone(request.parentPhone())
                .profileImage(profileImageUrl) // URL 저장
                .role("ROLE_USER")
                .status("ACTIVE")
                .membershipTier(MembershipTier.BASIC) // 기본 등급
                .totalPoint(0)
                .pointBalance(0)
                .level(1)
                .exp(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userMapper.save(newUser);
    }

    // --- [3] 소셜 로그인 추가 정보 입력 ---
    @Transactional
    public AuthDTO.LoginResponse completeSocialSignup(String username, AuthDTO.SocialSignupRequest request, MultipartFile profileImage) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        // 1. 추가 정보 업데이트
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAge(request.age());
        user.setGender(request.gender());
        user.setParentPhone(request.parentPhone());
        user.setRole("ROLE_USER"); // 정회원 전환

        // 2. 프로필 이미지 업데이트 (업로드 된 경우만)
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String originalFilename = profileImage.getOriginalFilename();
                String ext = (originalFilename != null && originalFilename.contains("."))
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".jpg";
                String imageUrl = fileStore.storeFile(profileImage.getBytes(), ext);
                user.setProfileImage(imageUrl);
            } catch (Exception e) {
                log.error("소셜 가입 프로필 저장 실패: {}", e.getMessage());
            }
        }

        userMapper.updateSocialUser(user);

        // 3. 토큰 발급 (정회원 권한으로)
        String accessToken = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());

        redisTemplate.opsForValue().set("RT:" + user.getUsername(), refreshToken, 14, TimeUnit.DAYS);

        return AuthDTO.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .isNewUser(false)
                .build();
    }

    // --- [4] 중복 확인 ---
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return userMapper.findByUsername(username) == null;
    }

    // --- [5] 아이디 찾기 ---
    @Transactional(readOnly = true)
    public AuthDTO.AccountInfoResponse findUsername(AuthDTO.FindIdRequest request) {
        UserEntity user = userMapper.findByNameAndEmailAndPhone(
                request.name(), request.email(), request.phone());

        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        return new AuthDTO.AccountInfoResponse(user.getUsername(), "회원님의 아이디를 찾았습니다.", true);
    }

    // --- [6] 임시 비밀번호 발송 ---
    @Transactional
    public void sendTemporaryPassword(AuthDTO.ResetPasswordRequest request) {
        UserEntity user = userMapper.findByUsernameAndEmail(request.username(), request.email());
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        String tempPw = UUID.randomUUID().toString().substring(0, 8);
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(tempPw));

        // 이메일 전송 (비동기 권장되나 여기선 동기 처리)
        emailService.sendTemporaryPassword(user.getEmail(), tempPw);
    }

    // --- [7] 이메일 인증 요청 ---
    public void requestEmailVerification(String email) {
        emailService.sendVerificationCode(email);
    }

    // --- [8] 이메일 인증 검증 ---
    public boolean verifyEmailCode(String email, String code) {
        return emailService.verifyCode(email, code);
    }

    // --- [9] 토큰 재발급 ---
    public AuthDTO.LoginResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new TutorooException("유효하지 않은 리프레시 토큰입니다.", ErrorCode.INVALID_AUTH_CODE);
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String savedRt = redisTemplate.opsForValue().get("RT:" + username);

        if (savedRt == null || !savedRt.equals(refreshToken)) {
            throw new TutorooException("리프레시 토큰이 만료되었거나 일치하지 않습니다.", ErrorCode.INVALID_AUTH_CODE);
        }

        UserEntity user = userMapper.findByUsername(username);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());

        // Refresh Token Rotation (보안 강화: 재발급 시 RT도 갱신)
        redisTemplate.opsForValue().set("RT:" + username, newRefreshToken, 14, TimeUnit.DAYS);

        return AuthDTO.LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(username)
                .name(user.getName())
                .role(user.getRole())
                .isNewUser(false)
                .build();
    }
}