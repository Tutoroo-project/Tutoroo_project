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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final ChatClient.Builder chatClientBuilder; // AI ChatClient
    private final PasswordEncoder passwordEncoder;
    private final FileStore fileStore;

    // --- [1] 회원 정보 수정 ---
    @Transactional
    public void updateUserInfo(String currentUsername, UserDTO.UpdateRequest request, MultipartFile image) {
        UserEntity user = userMapper.findByUsername(currentUsername);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        // 현재 비밀번호 검증 (필수)
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new TutorooException("현재 비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
        }

        // 입력된 값만 변경 (Null 체크)
        if (StringUtils.hasText(request.newUsername())) user.setUsername(request.newUsername());
        if (StringUtils.hasText(request.newPassword())) user.setPassword(passwordEncoder.encode(request.newPassword()));
        if (StringUtils.hasText(request.email())) user.setEmail(request.email());
        if (StringUtils.hasText(request.phone())) user.setPhone(request.phone());

        // 프로필 이미지 업로드 처리
        if (image != null && !image.isEmpty()) {
            try {
                String imagePath = fileStore.storeFile(image.getBytes(), ".png");
                user.setProfileImage(imagePath);
            } catch (Exception e) {
                log.error("이미지 저장 실패: {}", e.getMessage());
                throw new TutorooException(ErrorCode.FILE_UPLOAD_ERROR);
            }
        }

        userMapper.update(user);
    }

    // --- [2] 대시보드 조회 (AI 분석 포함) ---
    @Transactional(readOnly = true)
    public UserDTO.DashboardDTO getAdvancedDashboard(String username) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(user.getId());
        StudyPlanEntity currentPlan = plans.isEmpty() ? null : plans.get(0);

        String aiAnalysis = "현재 진행 중인 학습 목표가 없습니다.";
        String aiSuggestion = "새로운 목표를 설정해보세요!";
        List<Integer> weeklyScores = new ArrayList<>();

        if (currentPlan != null) {
            List<StudyLogEntity> recentLogs = studyMapper.findLogsByPlanId(currentPlan.getId());
            weeklyScores = recentLogs.stream().limit(7).map(StudyLogEntity::getTestScore).collect(Collectors.toList());

            try {
                if (!recentLogs.isEmpty()) {
                    String logSummary = recentLogs.stream().limit(3)
                            .map(l -> l.getDayCount() + "일차: " + l.getTestScore() + "점")
                            .collect(Collectors.joining(", "));

                    // AI 분석 요청
                    String prompt = String.format("학생: [%s], 목표: [%s], 최근 기록: [%s]. 150자 이내로 성취도 분석과 격려의 말을 해줘.",
                            user.getName(), currentPlan.getGoal(), logSummary);
                    aiAnalysis = chatClientBuilder.build().prompt().user(prompt).call().content();
                    aiSuggestion = "꾸준함이 답입니다! 오늘도 화이팅!"; // 간단한 제안 (또는 AI에게 추가 요청 가능)
                }
            } catch (Exception e) {
                log.error("AI 리포트 생성 중 오류: {}", e.getMessage());
                aiAnalysis = "AI 분석 서버 연결이 지연되고 있습니다.";
            }
        }

        return UserDTO.DashboardDTO.builder()
                .name(user.getName())
                .currentGoal(currentPlan != null ? currentPlan.getGoal() : "목표 없음")
                .progressRate(currentPlan != null ? currentPlan.getProgressRate() : 0.0)
                .currentPoint(user.getTotalPoint()) // 누적 포인트 표시
                .rank(user.getDailyRank() != null ? user.getDailyRank() : 0)
                .aiAnalysisReport(aiAnalysis)
                .aiSuggestion(aiSuggestion)
                .weeklyScores(weeklyScores)
                .build();
    }

    // --- [3] 라이벌 매칭 ---
    @Transactional
    public String matchRival(Long userId) {
        UserEntity me = userMapper.findById(userId);
        if (me.getRivalId() != null) return "이미 라이벌이 등록되어 있습니다.";

        // 점수가 비슷한(+/- 200점) 상대를 찾음 (Mapper 쿼리 활용)
        UserEntity rival = userMapper.findPotentialRival(me.getId(), me.getTotalPoint());
        if (rival == null) return "현재 매칭 가능한 라이벌이 없습니다.";

        me.setRivalId(rival.getId());
        userMapper.update(me);
        return "매칭 성공! 라이벌: " + rival.getMaskedName();
    }

    // --- [4] 회원 탈퇴 (Soft Delete) ---
    @Transactional
    public void withdrawUser(Long userId, UserDTO.WithdrawRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        // 소셜 로그인 회원이 아닐 경우 비밀번호 확인
        if (user.getProvider() == null) {
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new TutorooException("비밀번호가 일치하지 않아 탈퇴할 수 없습니다.", ErrorCode.INVALID_PASSWORD);
            }
        }

        // Soft Delete 처리
        user.setStatus("WITHDRAWN");
        user.setWithdrawalReason(request.reason());
        user.setDeletedAt(LocalDateTime.now()); // 탈퇴 신청일 기록 (스케줄러가 90일 후 삭제함)

        userMapper.update(user);
    }
}