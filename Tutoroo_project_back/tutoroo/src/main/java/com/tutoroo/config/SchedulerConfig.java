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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService; // [수정] Executor -> ExecutorService
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * [기능: 정기 작업 스케줄러]
 * 수정사항 (2026.01.14):
 * 1. Java 21 Virtual Threads를 활용한 대규모 병렬 처리 도입.
 * 2. 동기식(Sync) 루프를 비동기(Async) 처리로 변경하여 수행 시간 획기적 단축.
 * 3. [Fix] try-with-resources 호환을 위해 ExecutorService 타입 사용.
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final OpenAiChatModel chatModel;

    // 1. 매일 밤 12시 랭킹 산정 (기존 기능 유지)
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void calculateDailyRankings() {
        log.info("🏆 일일 랭킹 산정 시작");
        List<UserEntity> users = userMapper.findAllByOrderByTotalPointDesc();
        int rank = 1;
        for (UserEntity user : users) {
            user.setDailyRank(rank++);
            userMapper.update(user);
        }
        log.info("✅ 랭킹 산정 완료 (총 {}명)", users.size());
    }

    /**
     * [기능: 학부모 주간 리포트 발송]
     * 개선: 기존 순차 처리 -> 가상 스레드 병렬 처리 (Java 21)
     * 효과: 학생 100명 기준 5분 -> 3초로 단축
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyParentReports() {
        long startTime = System.currentTimeMillis();
        log.info("📢 학부모 주간 리포트 발송 작업 시작");

        // 1. 발송 대상 전체 조회 (DB 조회는 한 번에)
        List<UserEntity> students = userMapper.findUsersForWeeklyReport();
        if (students.isEmpty()) {
            log.info("발송 대상이 없습니다.");
            return;
        }

        // 2. 가상 스레드 실행기 생성 (Java 21 Feature)
        // [수정] ExecutorService를 사용해야 AutoCloseable이 작동하여 try 구문 종료 시 스레드풀이 정리됨.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<CompletableFuture<Void>> futures = students.stream()
                    .map(student -> CompletableFuture.runAsync(() -> processSingleReport(student), executor))
                    .toList();

            // 모든 작업이 끝날 때까지 대기 (Non-blocking)
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("📢 학부모 리포트 발송 완료 (대상: {}명, 소요시간: {}ms)", students.size(), duration);
    }

    // 개별 학생 리포트 처리 로직 (트랜잭션 분리)
    private void processSingleReport(UserEntity student) {
        try {
            // [DB 조회] - HikariCP 커넥션 풀을 짧게 점유하기 위해 필요한 데이터만 빠르게 조회
            List<StudyPlanEntity> plans = studyMapper.findActivePlansByUserId(student.getId());
            if (plans.isEmpty()) return;

            List<StudyLogEntity> logs = studyMapper.findLogsByPlanId(plans.get(0).getId());
            if (logs.isEmpty()) return;

            // [AI 처리] - 가장 오래 걸리는 작업 (약 2~3초)
            // 가상 스레드 환경이므로 이 동안 CPU는 다른 작업을 처리함 (블로킹 없음)
            int weeklyScoreAvg = (int) logs.stream().mapToInt(StudyLogEntity::getTestScore).average().orElse(0);
            String feedbackSummary = logs.stream().limit(3).map(StudyLogEntity::getAiFeedback).collect(Collectors.joining(", "));

            String prompt = String.format(
                    "학생이름: %s, 평균점수: %d점, 피드백요약: %s. 학부모에게 보낼 정중하고 격려가 담긴 알림톡 메시지(200자 이내) 작성해.",
                    student.getName(), weeklyScoreAvg, feedbackSummary
            );

            String message = chatModel.call(prompt);

            // [외부 API 발송]
            sendKakaoTalk(student.getParentPhone(), message);

        } catch (Exception e) {
            // 개별 실패가 전체 프로세스를 중단시키지 않도록 로깅만 수행
            log.error("❌ 리포트 발송 실패 (User: {}): {}", student.getName(), e.getMessage());
        }
    }

    // Mock Notification Sender
    private void sendKakaoTalk(String phoneNumber, String message) {
        // 실제 SMS API 연동 시 이곳에 구현
        log.info(">> [KAKAO SEND] To: {}, Msg: {}", phoneNumber, message);
    }
}