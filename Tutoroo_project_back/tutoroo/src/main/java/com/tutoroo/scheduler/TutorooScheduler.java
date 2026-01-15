package com.tutoroo.scheduler;

import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.service.NotificationService;
import com.tutoroo.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TutorooScheduler {

    private final UserMapper userMapper;
    private final PetService petService;
    private final NotificationService notificationService;

    // [핵심] AsyncConfig에서 정의한 가상 스레드 실행기 주입
    private final AsyncTaskExecutor taskExecutor;

    // 1. [AI 감성] 매일 밤 자정 펫 일기 작성 (병렬 처리 버전)
    @Scheduled(cron = "0 0 0 * * *")
    public void runMidnightDiaryRoutine() {
        log.info("🌙 [스케줄러] 펫들의 한밤중 일기 쓰기 시작 (Virtual Threads)");

        List<UserEntity> activeUsers = userMapper.findAllByOrderByTotalPointDesc();

        // [보완] 순차 처리 -> 비동기 병렬 처리로 변경
        // 가상 스레드를 사용하므로 수천 개의 스레드를 생성해도 리소스 부담이 거의 없습니다.
        List<CompletableFuture<Void>> futures = activeUsers.stream()
                .map(user -> CompletableFuture.runAsync(() -> {
                    try {
                        // 트랜잭션은 writeMidnightDiary 메서드 내부에서 시작되고 끝남
                        petService.writeMidnightDiary(user.getId());
                    } catch (Exception e) {
                        log.error("❌ 일기 작성 실패 (User: {}): {}", user.getId(), e.getMessage());
                    }
                }, taskExecutor))
                .toList();

        // 모든 작업이 끝날 때까지 대기하지 않아도 된다면 아래 줄 생략 가능
        // CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("✅ [스케줄러] 총 {}명에 대한 일기 작성 요청을 백그라운드에서 처리 중입니다.", activeUsers.size());
    }

    // 2. [가출 시스템] (기존 로직 유지하되, 필요 시 동일하게 taskExecutor 적용 가능)
    @Scheduled(cron = "0 0 * * * *")
    public void checkRunawayStatus() {
        log.info("🚨 [스케줄러] 가출한 펫이 있는지 확인 중...");
        List<Long> runawayUserIds = petService.processBatchRunaways();

        for (Long userId : runawayUserIds) {
            // 알림 발송은 가벼운 작업이므로 여기서 바로 비동기 호출
            taskExecutor.execute(() ->
                    notificationService.send(userId, "펫이 가출했습니다! 😱 빨리 돌아와주세요!")
            );
        }
    }
}