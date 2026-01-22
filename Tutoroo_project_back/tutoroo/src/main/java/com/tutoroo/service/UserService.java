package com.tutoroo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoroo.dto.RivalDTO;
import com.tutoroo.dto.StudyDTO;
import com.tutoroo.dto.UserDTO;
import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.jwt.JwtTokenProvider;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileStore fileStore;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;

    // --- 0. 회원 상세 정보 조회 ---
    @Transactional(readOnly = true)
    public UserDTO.ProfileInfo getProfileInfo(String username) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        return toProfileInfo(user);
    }

    // --- 1. 회원 정보 수정 ---
    @Transactional
    public UserDTO.UpdateResponse updateUserInfo(String username, UserDTO.UpdateRequest request, MultipartFile image) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        String oldUsername = user.getUsername();

        // [검증] 소셜 로그인 유저는 비밀번호가 없으므로 패스, 일반 유저는 검증
        if (user.getProvider() == null) {
            if (request.currentPassword() != null && !request.currentPassword().isBlank()) {
                if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                    throw new TutorooException("현재 비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
                }
            }
        }

        // 1. [Snapshot] 변경 전 정보 저장
        UserDTO.ProfileInfo beforeInfo = toProfileInfo(user);

        // 2. 정보 업데이트
        // 비밀번호 변경 (Local 유저만 가능)
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (user.getProvider() != null) {
                throw new TutorooException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
            }
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        if (StringUtils.hasText(request.name())) user.setName(request.name());
        if (request.age() != null) user.setAge(request.age());
        if (StringUtils.hasText(request.phone())) user.setPhone(request.phone());

        // [중요] 이메일(아이디) 변경 처리
        if (StringUtils.hasText(request.email())) {
            if (!request.email().equals(oldUsername) && userMapper.findByUsername(request.email()) != null) {
                throw new TutorooException(ErrorCode.DUPLICATE_ID);
            }
            user.setEmail(request.email());
            user.setUsername(request.email());
        }

        // [핵심] 프로필 이미지 변경 (기존 파일 삭제 로직 추가)
        if (image != null && !image.isEmpty()) {
            try {
                // 1. 기존 이미지가 있다면 삭제 (쓰레기 파일 방지)
                if (StringUtils.hasText(user.getProfileImage())) {
                    fileStore.deleteFile(user.getProfileImage());
                }

                // 2. 새 파일 저장
                String originalFilename = image.getOriginalFilename();
                String ext = (originalFilename != null && originalFilename.contains("."))
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".jpg";

                String imageUrl = fileStore.storeFile(image.getBytes(), ext);
                user.setProfileImage(imageUrl);
            } catch (Exception e) {
                log.error("프로필 이미지 수정 실패: {}", e.getMessage());
                throw new TutorooException(ErrorCode.FILE_UPLOAD_ERROR);
            }
        }

        // 3. DB 반영
        userMapper.update(user);
        deleteDashboardCache(oldUsername);

        // 4. 아이디 변경 시 새 토큰 발급
        String newAccessToken = null;
        if (!oldUsername.equals(user.getUsername())) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    new CustomUserDetails(user), null, Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
            );
            newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            redisTemplate.delete("RT:" + oldUsername);
            redisTemplate.opsForValue().set("RT:" + user.getUsername(), newRefreshToken, 14, TimeUnit.DAYS);

            log.info("아이디 변경: {} -> {}", oldUsername, user.getUsername());
        }

        // 5. [Snapshot] 변경 후 정보
        UserDTO.ProfileInfo afterInfo = toProfileInfo(user);

        return UserDTO.UpdateResponse.builder()
                .before(beforeInfo)
                .after(afterInfo)
                .message("회원 정보가 성공적으로 변경되었습니다.")
                .accessToken(newAccessToken)
                .build();
    }

    // --- 2. 라이벌 비교 조회 ---
    @Transactional(readOnly = true)
    public RivalDTO.RivalComparisonResponse getRivalComparison(Long userId) {
        UserEntity me = userMapper.findById(userId);
        if (me == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        if (me.getRivalId() == null) {
            return RivalDTO.RivalComparisonResponse.builder()
                    .hasRival(false)
                    .myProfile(toRivalProfile(me))
                    .message("아직 라이벌이 없습니다. 매칭을 시작해보세요!")
                    .pointGap(0)
                    .build();
        }

        UserEntity rival = userMapper.findById(me.getRivalId());
        if (rival == null || !"ACTIVE".equals(rival.getStatus())) {
            return RivalDTO.RivalComparisonResponse.builder()
                    .hasRival(false)
                    .myProfile(toRivalProfile(me))
                    .message("라이벌이 떠났습니다. 새로운 라이벌을 찾아보세요.")
                    .pointGap(0)
                    .build();
        }

        int myScore = me.getTotalPoint();
        int rivalScore = rival.getTotalPoint();
        int gap = Math.abs(myScore - rivalScore);
        String msg = (myScore > rivalScore) ? "훌륭해요! 라이벌보다 " + gap + "점 앞서고 있습니다. 🏆" :
                (myScore < rivalScore) ? "분발하세요! 라이벌이 " + gap + "점 차이로 앞서갑니다. 🔥" :
                        "막상막하! 라이벌과 점수가 같습니다. 긴장하세요!";

        return RivalDTO.RivalComparisonResponse.builder()
                .hasRival(true)
                .myProfile(toRivalProfile(me))
                .rivalProfile(toRivalProfile(rival))
                .message(msg)
                .pointGap(gap)
                .build();
    }

    // --- 3. 대시보드 조회 ---
    @Transactional(readOnly = true)
    public UserDTO.DashboardDTO getAdvancedDashboard(String username) {
        String cacheKey = "dashboard:" + username;

        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, UserDTO.DashboardDTO.class);
            }
        } catch (Exception e) {
            log.warn("대시보드 캐시 조회 실패: {}", e.getMessage());
        }

        UserEntity user = userMapper.findByUsername(username);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(user.getId());

        List<StudyDTO.StudySimpleInfo> studyList = plans.stream()
                .map(plan -> StudyDTO.StudySimpleInfo.builder()
                        .id(plan.getId())
                        .name(plan.getGoal())
                        .tutor(plan.getCustomTutorName() != null ? plan.getCustomTutorName() : plan.getPersona())
                        .build())
                .collect(Collectors.toList());

        StudyPlanEntity currentPlan = plans.isEmpty() ? null : plans.get(0);
        String currentGoal = (currentPlan != null) ? currentPlan.getGoal() : "목표를 설정해주세요";
        double progressRate = (currentPlan != null) ? currentPlan.getProgressRate() : 0.0;

        List<StudyLogEntity> logs = (currentPlan != null)
                ? studyMapper.findLogsByPlanId(currentPlan.getId())
                : new ArrayList<>();

        List<Integer> weeklyScores = logs.stream()
                .skip(Math.max(0, logs.size() - 7))
                .map(StudyLogEntity::getTestScore)
                .collect(Collectors.toList());

        String aiAnalysis = "아직 충분한 학습 데이터가 없습니다. 꾸준히 학습해보세요!";
        String aiSuggestion = "오늘의 학습을 시작해보는 건 어때요?";

        if (!logs.isEmpty()) {
            StudyLogEntity lastLog = logs.get(logs.size() - 1);
            if (lastLog.getAiFeedback() != null) aiAnalysis = lastLog.getAiFeedback();
            aiSuggestion = "지난번 점수는 " + lastLog.getTestScore() + "점이었네요. 오늘은 더 잘할 수 있어요!";
        }

        UserDTO.DashboardDTO dashboardDTO = UserDTO.DashboardDTO.builder()
                .name(user.getName())
                .currentGoal(currentGoal)
                .progressRate(progressRate)
                .currentPoint(user.getTotalPoint())
                .rank(user.getDailyRank() != null ? user.getDailyRank() : 0)
                .aiAnalysisReport(aiAnalysis)
                .aiSuggestion(aiSuggestion)
                .weeklyScores(weeklyScores)
                .studyList(studyList)
                .build();

        try {
            String json = objectMapper.writeValueAsString(dashboardDTO);
            redisTemplate.opsForValue().set(cacheKey, json, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("대시보드 캐시 저장 실패: {}", e.getMessage());
        }

        return dashboardDTO;
    }

    // --- 4. 라이벌 매칭 ---
    @Transactional
    public String matchRival(Long userId) {
        UserEntity me = userMapper.findById(userId);
        if (me.getRivalId() != null) return "이미 라이벌이 등록되어 있습니다.";

        UserEntity rival = userMapper.findPotentialRival(me.getId(), me.getTotalPoint());
        if (rival == null) return "현재 매칭 가능한 라이벌이 없습니다.";

        me.setRivalId(rival.getId());
        userMapper.update(me);
        deleteDashboardCache(me.getUsername());

        return "매칭 성공! 라이벌: " + rival.getMaskedName();
    }

    // --- 5. 회원 탈퇴 ---
    @Transactional
    public void withdrawUser(Long userId, UserDTO.WithdrawRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        if (user.getProvider() == null) {
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new TutorooException("비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
            }
        }

        user.setStatus("WITHDRAWN");
        user.setWithdrawalReason(request.reason());
        user.setDeletedAt(LocalDateTime.now());
        userMapper.update(user);

        deleteDashboardCache(user.getUsername());
        redisTemplate.delete("RT:" + user.getUsername());
    }

    // --- 6. 비밀번호 검증 ---
    @Transactional(readOnly = true)
    public void verifyPassword(Long userId, String rawPassword) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        if (user.getProvider() != null) return;

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new TutorooException("비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
        }
    }

    // --- Helper Methods ---
    private void deleteDashboardCache(String username) {
        try {
            redisTemplate.delete("dashboard:" + username);
        } catch (Exception e) {}
    }

    // Entity -> ProfileInfo 변환 (중복 제거)
    private UserDTO.ProfileInfo toProfileInfo(UserEntity user) {
        return UserDTO.ProfileInfo.builder()
                .username(user.getUsername())
                .name(user.getName())
                .age(user.getAge())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .membershipTier(user.getEffectiveTier().name())
                .provider(user.getProvider()) // [New] 소셜 유저 여부 추가
                .build();
    }

    private RivalDTO.RivalProfile toRivalProfile(UserEntity user) {
        return RivalDTO.RivalProfile.builder()
                .userId(user.getId())
                .name(user.getMaskedName())
                .profileImage(user.getProfileImage())
                .totalPoint(user.getTotalPoint())
                .rank(user.getDailyRank() != null ? user.getDailyRank() : 0)
                .level(user.getLevel())
                .tier(user.getEffectiveTier().name())
                .build();
    }

    // [New] 결제용 유저 정보 조회
    @Transactional(readOnly = true)
    public UserDTO.PaymentUserInfo getPaymentUserInfo(Long userId) {
        UserEntity user = userMapper.findById(userId); // 또는 userRepository.findById(userId)

        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        return UserDTO.PaymentUserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }


}