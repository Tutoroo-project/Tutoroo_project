package com.tutoroo.config;

import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

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

    /**
     * [주간 리포트 발송 스케줄러]
     * 동작 시간: 매주 월요일 오전 9시
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyReport() {
        log.info("📢 [Scheduler] 주간 리포트 발송 시작");

        List<UserEntity> targetUsers = userMapper.findUsersForWeeklyReport();
        for (UserEntity user : targetUsers) {
            try {
                processAndSendReport(user);
            } catch (Exception e) {
                log.error("❌ 리포트 생성 실패 (학생: {}): {}", user.getName(), e.getMessage());
            }
        }
        log.info("✅ [Scheduler] 주간 리포트 발송 종료");
    }

    /**
     * [요구사항 2] 탈퇴 회원 영구 삭제 스케줄러
     * 동작 시간: 매일 새벽 4시
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeWithdrawnUsers() {
        log.info("🗑️ [Scheduler] 탈퇴 회원 영구 삭제 작업 시작");

        // 탈퇴한지 90일 지난 유저 조회 (쿼리는 UserMapper.xml 참조)
        // XML에 <select id="findWithdrawnUsersForPurge"> 구현되어 있음
        List<UserEntity> usersToDelete = userMapper.findWithdrawnUsersForPurge();

        int count = 0;
        for (UserEntity user : usersToDelete) {
            userMapper.deleteUserPermanently(user.getId()); // XML에 구현 필요
            count++;
        }

        log.info("✅ [Scheduler] 총 {}명의 탈퇴 회원 데이터가 영구 삭제되었습니다.", count);
    }

    private void processAndSendReport(UserEntity student) {
        List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(student.getId());
        if (plans.isEmpty()) return;

        List<StudyLogEntity> logs = studyMapper.findLogsByPlanId(plans.get(0).getId());
        if (logs.isEmpty()) return;

        int weeklyScoreAvg = (int) logs.stream().mapToInt(StudyLogEntity::getTestScore).average().orElse(0);
        String feedbackSummary = logs.stream().limit(3).map(StudyLogEntity::getAiFeedback).collect(Collectors.joining(", "));

        String prompt = String.format(
                "학생이름: %s, 평균점수: %d점, 피드백요약: %s. 학부모에게 보낼 정중하고 격려가 담긴 알림톡 메시지(200자 이내) 작성해.",
                student.getName(), weeklyScoreAvg, feedbackSummary
        );

        String message = chatModel.call(prompt);
        sendKakaoTalk(student.getParentPhone(), message);
    }

    private void sendKakaoTalk(String phoneNumber, String message) {
        log.info("📩 [알림톡 발송] To: {}, 내용: {}", phoneNumber, message);
    }
}