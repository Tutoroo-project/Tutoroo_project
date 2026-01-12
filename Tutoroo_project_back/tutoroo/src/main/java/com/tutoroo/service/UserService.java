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

    /**
     * [기능: 사용자 정보 수정]
     * 설명: 기존 기능을 그대로 유지합니다.
     */
    @Transactional
    public void updateUserInfo(String username, UserDTO.UpdateRequest request) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        user.setPhone(request.getPhone());
        userMapper.updateUserContact(user.getId(), user.getPhone());
    }

    /**
     * [기능: 대시보드 조회 (멤버십 등급별 리포트 차별화 적용)]
     */
    @Transactional(readOnly = true)
    public DashboardDTO getAdvancedDashboard(String username) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            throw new TutorooException(ErrorCode.USER_NOT_FOUND);
        }

        // [신규] 현재 사용자의 유효 멤버십 등급 조회
        MembershipTier tier = user.getEffectiveTier();

        // 1. 학습 플랜 조회
        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(user.getId());

        // 2. 학습 플랜이 없는 경우 (기존 로직 유지)
        if (plans.isEmpty()) {
            return DashboardDTO.builder()
                    .name(user.getName())
                    .currentPoint(user.getTotalPoint())
                    .progressRate(0.0)
                    .currentGoal("목표 없음")
                    .aiAnalysisReport("캥거루 선생님과 함께 첫 번째 학습 목표를 설정해보세요! 🦘")
                    .aiSuggestion("상단 메뉴에서 [상담 시작하기]를 눌러보세요.")
                    .weeklyScores(List.of())
                    .recentFeedbacks(List.of()) // DTO 필드에 따라 빈 리스트 반환
                    .build();
        }

        // 3. 최근 학습 로그 조회 (기존 로직 유지)
        StudyPlanEntity currentPlan = plans.get(0);
        List<StudyLogEntity> recentLogs = studyMapper.findLogsByPlanId(currentPlan.getId());

        // 4. 학습 기록 요약 문자열 생성
        String logSummary = recentLogs.stream()
                .limit(5)
                .map(log -> "점수: " + log.getTestScore() + " 피드백: " + log.getAiFeedback())
                .collect(Collectors.joining(" | "));

        // 기본값 설정
        String aiAnalysis = "아직 분석할 데이터가 충분하지 않습니다.";
        String aiSuggestion = "꾸준히 학습을 진행해주세요!";

        // 5. [핵심 변경] 멤버십 등급에 따른 분석 리포트 생성 분기
        if (!recentLogs.isEmpty()) {
            try {
                switch (tier.getReportDetailLevel()) {
                    case "SIMPLE" -> {
                        // [BASIC 등급]
                        // API 호출을 하지 않아 비용을 절약하고, 업그레이드 유도 문구 출력
                        aiAnalysis = String.format("%s님, 꾸준히 학습하고 계시네요! 더 상세한 AI 정밀 분석을 원하시면 Premium으로 업그레이드 해보세요.", user.getName());
                        aiSuggestion = "오늘도 목표를 향해 파이팅하세요!";
                    }
                    case "WEEKLY" -> {
                        // [STANDARD 등급]
                        // 간단한 주간 요약 프롬프트 실행
                        String prompt = String.format("""
                            학생 목표: %s
                            최근 기록: %s
                            지시사항: 위 데이터를 바탕으로 학생의 '강점'과 '약점'을 각각 한 문장으로 간단히 요약해줘.
                            형식: 강점: ..., 약점: ...
                            """, currentPlan.getGoal(), logSummary);
                        aiAnalysis = chatModel.call(prompt);
                        aiSuggestion = "약점을 보완하기 위해 복습 퀴즈를 풀어보세요.";
                    }
                    case "DEEP" -> {
                        // [PREMIUM 등급]
                        // 심층 분석 및 솔루션 제안 프롬프트 실행 (기존 고급 로직)
                        String prompt = String.format("""
                            학생: [%s], 목표: [%s]
                            학습 기록: [%s]
                            지시사항: 위 데이터를 심층 분석하여 다음 두 가지 항목으로 나누어 답변하세요.
                            1. 분석: 성취도 추이와 구체적인 취약점 분석
                            2. 제안: 향후 1주일간의 구체적인 학습 솔루션
                            출력 형식: 분석: [내용] / 제안: [내용]
                            """, user.getName(), currentPlan.getGoal(), logSummary);

                        String aiResponse = chatModel.call(prompt);

                        // 응답 파싱
                        String[] parts = aiResponse.split("제안:");
                        aiAnalysis = parts[0].replace("분석:", "").trim();
                        if (parts.length > 1) {
                            aiSuggestion = parts[1].trim();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("AI 리포트 생성 중 오류: {}", e.getMessage());
                aiAnalysis = "일시적으로 AI 분석을 불러올 수 없습니다.";
            }
        }

        // 6. 결과 반환 (기존 구조 유지)
        return DashboardDTO.builder()
                .name(user.getName())
                .currentGoal(currentPlan.getGoal())
                .progressRate(currentPlan.getProgressRate())
                .currentPoint(user.getTotalPoint())
                .aiAnalysisReport(aiAnalysis)
                .aiSuggestion(aiSuggestion)
                .weeklyScores(recentLogs.stream()
                        .limit(7)
                        .map(StudyLogEntity::getTestScore)
                        .collect(Collectors.toList()))
                .build();
    }
}