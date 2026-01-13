package com.tutoroo.config;

import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final OpenAiChatModel chatModel;

    // 1. 매일 밤 12시 랭킹 산정 (기존 기능)
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void calculateDailyRankings() {
        log.info("일일 랭킹 산정 시작");
        List<UserEntity> users = userMapper.findAllByOrderByTotalPointDesc();
        int rank = 1;
        for (UserEntity user : users) {
            user.setDailyRank(rank++);
            userMapper.update(user);
        }
        log.info("랭킹 산정 완료");
    }

    /**
     * [신규 기능 4] 학부모 주간 리포트 발송
     * 주기: 매주 월요일 오전 9시
     */
    @Transactional
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyParentReports() {
        log.info("📢 학부모 주간 리포트 발송 작업 시작");

        // 1. 리포트 발송 대상 조회 (부모 번호가 있고 활동 기록이 있는 학생)
        List<UserEntity> students = userMapper.findUsersForWeeklyReport();

        for (UserEntity student : students) {
            try {
                // 2. 학생의 최근 1주일 학습 로그 조회
                var plans = studyMapper.findActivePlansByUserId(student.getId());
                if (plans.isEmpty()) continue;

                List<StudyLogEntity> logs = studyMapper.findLogsByPlanId(plans.get(0).getId());
                // (실무에서는 날짜 필터링 로직 추가 필요, 여기서는 최근 로그 사용)

                if (logs.isEmpty()) continue;

                // 3. AI 리포트 생성
                int weeklyScoreAvg = (int) logs.stream().mapToInt(StudyLogEntity::getTestScore).average().orElse(0);
                String feedbackSummary = logs.stream().limit(3).map(StudyLogEntity::getAiFeedback).collect(Collectors.joining(", "));

                String prompt = String.format("학생이름: %s, 평균점수: %d점, 피드백요약: %s. 학부모에게 보낼 정중한 알림톡 메시지(300자 이내) 작성해.",
                        student.getName(), weeklyScoreAvg, feedbackSummary);

                String message = chatModel.call(prompt);

                // 4. 리포트 발송 (외부 SMS/카톡 API 연동 포인트)
                sendKakaoTalk(student.getParentPhone(), message);

            } catch (Exception e) {
                log.error("리포트 발송 실패 (User: {}): {}", student.getUsername(), e.getMessage());
            }
        }
        log.info("📢 학부모 리포트 발송 완료");
    }

    // Mock Notification Sender
    private void sendKakaoTalk(String phoneNumber, String message) {
        log.info(">> [KAKAO SEND] To: {}, Content: {}", phoneNumber, message);
    }
}