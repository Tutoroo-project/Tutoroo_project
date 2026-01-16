package com.tutoroo.service;

import com.tutoroo.dto.UserDTO;
import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.util.FileStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ObjectMapper objectMapper; // JSON 캐싱용

    // --- [1] 회원 정보 수정 ---
    @Transactional
    public void updateUserInfo(String username, UserDTO.UpdateRequest request, MultipartFile image) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        // 보안: 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new TutorooException(ErrorCode.INVALID_PASSWORD);
        }

        // 정보 업데이트
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        if (request.email() != null && !request.email().isBlank()) user.setEmail(request.email());
        if (request.phone() != null && !request.phone().isBlank()) user.setPhone(request.phone());

        // 프로필 이미지 변경 (FileStore 사용)
        if (image != null && !image.isEmpty()) {
            try {
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

        userMapper.update(user);

        // [캐시 무효화] 정보가 바뀌었으므로 대시보드 캐시 삭제
        deleteDashboardCache(username);
    }

    // --- [2] 대시보드 조회 (Redis 캐싱 적용) ---
    @Transactional(readOnly = true)
    public UserDTO.DashboardDTO getAdvancedDashboard(String username) {
        String cacheKey = "dashboard:" + username;

        // 1. Redis 캐시 확인
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, UserDTO.DashboardDTO.class);
            }
        } catch (Exception e) {
            log.warn("대시보드 캐시 조회 실패 (DB에서 조회 진행): {}", e.getMessage());
        }

        // 2. DB 조회 및 DTO 생성 (캐시 Miss)
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        // 진행 중인 플랜 조회
        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(user.getId());
        StudyPlanEntity currentPlan = plans.isEmpty() ? null : plans.get(0);

        String currentGoal = (currentPlan != null) ? currentPlan.getGoal() : "목표를 설정해주세요";
        double progressRate = (currentPlan != null) ? currentPlan.getProgressRate() : 0.0;

        // 최근 학습 로그 조회 (AI 피드백 및 점수용)
        List<StudyLogEntity> logs = (currentPlan != null)
                ? studyMapper.findLogsByPlanId(currentPlan.getId())
                : new ArrayList<>();

        // 최근 7개 점수 추출
        List<Integer> weeklyScores = logs.stream()
                .skip(Math.max(0, logs.size() - 7))
                .map(StudyLogEntity::getTestScore)
                .collect(Collectors.toList());

        // 최근 AI 피드백 추출 (없으면 기본 메시지)
        String aiAnalysis = "아직 충분한 학습 데이터가 없습니다. 꾸준히 학습해보세요!";
        String aiSuggestion = "오늘의 학습을 시작해보는 건 어때요?";

        if (!logs.isEmpty()) {
            StudyLogEntity lastLog = logs.get(logs.size() - 1);
            if (lastLog.getAiFeedback() != null) aiAnalysis = lastLog.getAiFeedback();
            // 제안 로직은 간단히 처리 (실시간 AI 호출 비용 절감)
            aiSuggestion = "지난번 점수는 " + lastLog.getTestScore() + "점이었네요. 오늘은 더 잘할 수 있어요!";
        }

        UserDTO.DashboardDTO dashboardDTO = UserDTO.DashboardDTO.builder()
                .name(user.getName())
                .currentGoal(currentGoal)
                .progressRate(progressRate)
                .currentPoint(user.getTotalPoint()) // 누적 포인트 표시
                .rank(user.getDailyRank() != null ? user.getDailyRank() : 0)
                .aiAnalysisReport(aiAnalysis)
                .aiSuggestion(aiSuggestion)
                .weeklyScores(weeklyScores)
                .build();

        // 3. Redis 캐시 저장 (유효기간 10분)
        try {
            String json = objectMapper.writeValueAsString(dashboardDTO);
            redisTemplate.opsForValue().set(cacheKey, json, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("대시보드 캐시 저장 실패: {}", e.getMessage());
        }

        return dashboardDTO;
    }

    // --- [3] 라이벌 매칭 ---
    @Transactional
    public String matchRival(Long userId) {
        UserEntity me = userMapper.findById(userId);
        if (me.getRivalId() != null) return "이미 라이벌이 등록되어 있습니다.";

        // 점수가 비슷한(+/- 200점) 상대를 찾음
        UserEntity rival = userMapper.findPotentialRival(me.getId(), me.getTotalPoint());
        if (rival == null) return "현재 매칭 가능한 라이벌이 없습니다.";

        me.setRivalId(rival.getId());
        userMapper.update(me);

        deleteDashboardCache(me.getUsername()); // 상태 변경 캐시 삭제
        return "매칭 성공! 라이벌: " + rival.getMaskedName();
    }

    // --- [4] 회원 탈퇴 ---
    @Transactional
    public void withdrawUser(Long userId, UserDTO.WithdrawRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        // 소셜 회원이 아닌 경우 비밀번호 검증
        if (user.getProvider() == null) {
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new TutorooException("비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
            }
        }

        // Soft Delete 처리
        user.setStatus("WITHDRAWN");
        user.setWithdrawalReason(request.reason());
        user.setDeletedAt(LocalDateTime.now());

        userMapper.update(user);

        // 관련 캐시 및 토큰 정리
        deleteDashboardCache(user.getUsername());
        redisTemplate.delete("RT:" + user.getUsername());
    }

    // [Helper] 캐시 삭제 메서드
    private void deleteDashboardCache(String username) {
        try {
            redisTemplate.delete("dashboard:" + username);
        } catch (Exception e) {
            log.warn("캐시 삭제 실패: {}", e.getMessage());
        }
    }
}