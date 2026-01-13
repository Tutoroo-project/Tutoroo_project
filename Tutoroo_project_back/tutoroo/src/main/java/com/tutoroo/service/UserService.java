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

    /**
     * [기능: 사용자 정보 수정]
     */
    @Transactional
    public void updateUserInfo(String username, UserDTO.UpdateRequest request) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getProvider() == null) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new TutorooException("비밀번호를 입력해주세요.", ErrorCode.INVALID_INPUT_VALUE);
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("회원정보 수정 실패: 비밀번호 불일치 (User: {})", username);
                throw new TutorooException(ErrorCode.INVALID_PASSWORD);
            }
        }

        user.setPhone(request.getPhone());
        userMapper.updateUserContact(user.getId(), user.getPhone());

        log.info("사용자 정보 수정 완료: User={}", username);
    }

    /**
     * [기능: 대시보드 조회]
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
                    case "SIMPLE" -> {
                        aiAnalysis = String.format("%s님, 꾸준히 학습하고 계시네요! 더 상세한 분석을 위해 Premium으로 업그레이드 해보세요.", user.getName());
                    }
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
     * [신규 기능 2] 라이벌 매칭 시스템
     * 설명: 나와 점수대가 비슷한 유저를 찾아 라이벌로 등록합니다.
     */
    @Transactional
    public String matchRival(Long userId) {
        UserEntity me = userMapper.findById(userId);
        if (me.getRivalId() != null) {
            return "이미 라이벌이 등록되어 있습니다.";
        }

        // 나랑 점수가 +- 200점 차이나는 유저 찾기
        UserEntity rival = userMapper.findPotentialRival(me.getId(), me.getTotalPoint());

        if (rival == null) {
            return "현재 매칭 가능한 라이벌이 없습니다. 열심히 공부해서 점수를 올려보세요!";
        }

        // 서로 라이벌 등록 (단방향 or 양방향 - 여기선 내 쪽에만 등록)
        me.setRivalId(rival.getId());
        userMapper.update(me);

        return "매칭 성공! 새로운 라이벌: " + rival.getMaskedName() + " (점수: " + rival.getTotalPoint() + "점)";
    }
}