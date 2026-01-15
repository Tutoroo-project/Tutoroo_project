package com.tutoroo.service;

import com.tutoroo.dto.DashboardDTO;
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
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final OpenAiChatModel chatModel;
    private final PasswordEncoder passwordEncoder;
    private final FileStore fileStore;

    @Transactional
    public void updateUserInfo(String currentUsername, UserDTO.UpdateRequest request, MultipartFile image) {
        UserEntity user = userMapper.findByUsername(currentUsername);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new TutorooException("현재 비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
        }

        if (StringUtils.hasText(request.newUsername())) user.setUsername(request.newUsername());
        if (StringUtils.hasText(request.newPassword())) user.setPassword(passwordEncoder.encode(request.newPassword()));
        if (StringUtils.hasText(request.email())) user.setEmail(request.email());
        if (StringUtils.hasText(request.phone())) user.setPhone(request.phone());

        if (image != null && !image.isEmpty()) {
            String imagePath = fileStore.storeFile(getImageBytes(image), ".png");
            user.setProfileImage(imagePath);
        }

        userMapper.update(user);
    }

    private byte[] getImageBytes(MultipartFile file) {
        try { return file.getBytes(); }
        catch (Exception e) { throw new RuntimeException("이미지 처리 실패"); }
    }

    @Transactional(readOnly = true)
    public DashboardDTO getAdvancedDashboard(String username) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(user.getId());
        StudyPlanEntity currentPlan = plans.isEmpty() ? null : plans.get(0);

        String aiAnalysis = "현재 진행 중인 학습 목표가 없습니다.";
        String aiSuggestion = "새로운 목표를 설정해보세요!";
        List<StudyLogEntity> recentLogs = List.of();

        if (currentPlan != null) {
            recentLogs = studyMapper.findLogsByPlanId(currentPlan.getId());
            try {
                if (!recentLogs.isEmpty()) {
                    String logSummary = recentLogs.stream().limit(3)
                            .map(l -> l.getDayCount() + "일차: " + l.getTestScore() + "점")
                            .collect(Collectors.joining(", "));

                    String prompt = String.format("학생: [%s], 목표: [%s], 기록: [%s]. 성취도 분석과 향후 1주일 솔루션을 제안해.", user.getName(), currentPlan.getGoal(), logSummary);
                    aiAnalysis = chatModel.call(prompt);
                }
            } catch (Exception e) {
                log.error("AI 리포트 생성 중 오류: {}", e.getMessage());
            }
        }

        return DashboardDTO.builder()
                .name(user.getName())
                .currentGoal(currentPlan != null ? currentPlan.getGoal() : "목표 없음")
                .progressRate(currentPlan != null ? currentPlan.getProgressRate() : 0.0)
                .currentPoint(user.getTotalPoint())
                .aiAnalysisReport(aiAnalysis)
                .aiSuggestion(aiSuggestion)
                .weeklyScores(recentLogs.stream().limit(7).map(StudyLogEntity::getTestScore).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public String matchRival(Long userId) {
        UserEntity me = userMapper.findById(userId);
        if (me.getRivalId() != null) return "이미 라이벌이 등록되어 있습니다.";

        UserEntity rival = userMapper.findPotentialRival(me.getId(), me.getTotalPoint());
        if (rival == null) return "현재 매칭 가능한 라이벌이 없습니다.";

        me.setRivalId(rival.getId());
        userMapper.update(me);
        return "매칭 성공! 라이벌: " + rival.getMaskedName();
    }

    // [요구사항 2] 회원 탈퇴 처리 (Soft Delete)
    @Transactional
    public void withdrawUser(Long userId, String password, String reason) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) throw new TutorooException(ErrorCode.USER_NOT_FOUND);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new TutorooException("비밀번호가 일치하지 않아 탈퇴할 수 없습니다.", ErrorCode.INVALID_PASSWORD);
        }

        user.setStatus("WITHDRAWN");
        user.setWithdrawalReason(reason);
        user.setDeletedAt(LocalDateTime.now().plusDays(90)); // 90일 뒤 삭제 예정일 마킹 (혹은 현재시간 저장 후 쿼리에서 비교)
        // 여기서는 명확하게 쿼리에서 비교하기 위해 deletedAt에 '탈퇴 시점'을 넣겠습니다.
        user.setDeletedAt(LocalDateTime.now());

        userMapper.update(user);
    }
}