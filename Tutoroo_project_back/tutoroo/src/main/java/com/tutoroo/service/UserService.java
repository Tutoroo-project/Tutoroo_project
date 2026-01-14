package com.tutoroo.service;

import com.tutoroo.dto.DashboardDTO;
import com.tutoroo.dto.UserDTO;
import com.tutoroo.entity.MembershipTier;
import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final OpenAiChatModel chatModel;
    private final PasswordEncoder passwordEncoder;

    /**
     * [기능: 회원정보 통합 수정 로직]
     * 조건: 민감 정보(ID, PW, Email, Phone) 수정 시 현재 비밀번호 확인 필수
     */
    @Transactional
    public void updateUserInfo(String currentUsername, UserDTO.UpdateRequest request, MultipartFile image) {
        // 1. 현재 로그인된 사용자 조회
        UserEntity user = userMapper.findByUsername(currentUsername);
        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 민감한 정보 변경 여부 확인 (하나라도 값이 있으면 검증 필요)
        boolean isSensitiveChange = StringUtils.hasText(request.getNewUsername()) ||
                StringUtils.hasText(request.getNewPassword()) ||
                StringUtils.hasText(request.getEmail()) ||
                StringUtils.hasText(request.getPhone());

        // 소셜 로그인이 아닌 일반 유저라면 비밀번호 검증
        if (isSensitiveChange && user.getProvider() == null) {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new TutorooException("정보를 수정하려면 현재 비밀번호를 입력해주세요.", ErrorCode.INVALID_INPUT_VALUE);
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new TutorooException("현재 비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
            }
        }

        // 3. 정보 업데이트 시작

        // [아이디 변경] - 중복 체크 필수
        if (StringUtils.hasText(request.getNewUsername()) && !request.getNewUsername().equals(user.getUsername())) {
            UserEntity existingUser = userMapper.findByUsername(request.getNewUsername());
            if (existingUser != null) {
                throw new TutorooException("이미 사용 중인 아이디입니다.", ErrorCode.DUPLICATE_ID);
            }
            user.setUsername(request.getNewUsername());
            log.info("사용자 아이디 변경: {} -> {}", currentUsername, request.getNewUsername());
        }

        // [비밀번호 변경]
        if (StringUtils.hasText(request.getNewPassword())) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        // [이메일 변경]
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }

        // [휴대전화 번호 변경]
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }

        // [프로필 이미지 변경] - 자유롭게 변경 가능
        if (image != null && !image.isEmpty()) {
            // 실제 운영 환경에서는 S3 업로드 로직이 필요합니다.
            // 여기서는 파일명을 기반으로 경로만 저장하는 방식으로 처리합니다.
            String originalName = image.getOriginalFilename();
            String uuid = UUID.randomUUID().toString();
            String storedFileName = "/uploads/" + uuid + "_" + originalName;

            user.setProfileImage(storedFileName);
            log.info("프로필 이미지 업데이트: {}", storedFileName);
        }

        // 4. DB 저장
        userMapper.update(user);
    }

    /**
     * [기능: 대시보드 조회] (기존 코드 유지)
     */
    @Transactional(readOnly = true)
    public DashboardDTO getAdvancedDashboard(String username) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }
        MembershipTier tier = user.getEffectiveTier();
        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(user.getId());

        if (plans.isEmpty()) {
            return DashboardDTO.builder()
                    .name(user.getName())
                    .currentPoint(user.getTotalPoint())
                    .progressRate(0.0)
                    .currentGoal("목표 없음")
                    .aiAnalysisReport("캥거루 선생님과 함께 첫 번째 학습 목표를 설정해보세요! 🦘")
                    .aiSuggestion("상단 메뉴에서 [상담 시작하기]를 눌러보세요.")
                    .weeklyScores(List.of())
                    .build();
        }

        StudyPlanEntity currentPlan = plans.get(0);
        List<StudyLogEntity> recentLogs = studyMapper.findLogsByPlanId(currentPlan.getId());
        String logSummary = recentLogs.stream().limit(5)
                .map(log -> "점수: " + log.getTestScore() + " 피드백: " + log.getAiFeedback())
                .collect(Collectors.joining(" | "));

        String aiAnalysis = "아직 분석할 데이터가 충분하지 않습니다.";
        String aiSuggestion = "꾸준히 학습을 진행해주세요!";

        if (!recentLogs.isEmpty()) {
            try {
                switch (tier.getReportDetailLevel()) {
                    case "SIMPLE" -> aiAnalysis = String.format("%s님, 꾸준히 학습하고 계시네요! 더 상세한 분석을 위해 Premium으로 업그레이드 해보세요.", user.getName());
                    case "WEEKLY" -> {
                        String prompt = String.format("학생 목표: %s, 기록: %s. 강점과 약점을 한 문장씩 요약해.", currentPlan.getGoal(), logSummary);
                        aiAnalysis = chatModel.call(prompt);
                    }
                    case "DEEP" -> {
                        String prompt = String.format("학생: [%s], 목표: [%s], 기록: [%s]. 성취도 분석과 향후 1주일 솔루션을 제안해.", user.getName(), currentPlan.getGoal(), logSummary);
                        aiAnalysis = chatModel.call(prompt);
                    }
                }
            } catch (Exception e) {
                log.error("AI 리포트 생성 중 오류: {}", e.getMessage());
            }
        }

        return DashboardDTO.builder()
                .name(user.getName())
                .currentGoal(currentPlan.getGoal())
                .progressRate(currentPlan.getProgressRate())
                .currentPoint(user.getTotalPoint())
                .aiAnalysisReport(aiAnalysis)
                .aiSuggestion(aiSuggestion)
                .weeklyScores(recentLogs.stream().limit(7).map(StudyLogEntity::getTestScore).collect(Collectors.toList()))
                .build();
    }

    /**
     * [기능: 라이벌 매칭] (기존 코드 유지)
     */
    @Transactional
    public String matchRival(Long userId) {
        UserEntity me = userMapper.findById(userId);
        if (me.getRivalId() != null) {
            return "이미 라이벌이 등록되어 있습니다.";
        }
        UserEntity rival = userMapper.findPotentialRival(me.getId(), me.getTotalPoint());
        if (rival == null) {
            return "현재 매칭 가능한 라이벌이 없습니다. 열심히 공부해서 점수를 올려보세요!";
        }
        me.setRivalId(rival.getId());
        userMapper.update(me);
        return "매칭 성공! 새로운 라이벌: " + rival.getMaskedName() + " (점수: " + rival.getTotalPoint() + "점)";
    }
}