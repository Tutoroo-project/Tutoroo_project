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

    // AsyncConfig에서 정의한 가상 스레드 실행기 주입 (비동기 처리용)
    private final AsyncTaskExecutor taskExecutor;

    // 1. [AI 감성] 매일 밤 자정 펫 일기 작성 (병렬 처리)
    @Scheduled(cron = "0 0 0 * * *")
    public void runMidnightDiaryRoutine() {
        log.info("🌙 [스케줄러] 펫들의 한밤중 일기 쓰기 시작 (Virtual Threads)");

        List<UserEntity> activeUsers = userMapper.findAllByOrderByTotalPointDesc(); // 활동 중인 유저들

        // 가상 스레드를 활용해 수천 명 동시 처리 가능
        List<CompletableFuture<Void>> futures = activeUsers.stream()
                .map(user -> CompletableFuture.runAsync(() -> {
                    try {
                        petService.writeMidnightDiary(user.getId());
                    } catch (Exception e) {
                        log.error("❌ 일기 작성 실패 (User: {}): {}", user.getId(), e.getMessage());
                    }
                }, taskExecutor))
                .toList();

        // (선택) 모든 작업 완료 대기 로직이 필요하다면 여기에 추가

        log.info("✅ [스케줄러] 총 {}명에 대한 일기 작성 요청을 백그라운드에서 처리 중입니다.", activeUsers.size());
    }

    // 2. [가출 시스템] 매시간 실행
    @Scheduled(cron = "0 0 * * * *")
    public void checkRunawayStatus() {
        log.info("🚨 [스케줄러] 가출한 펫이 있는지 확인 중...");
        List<Long> runawayUserIds = petService.processBatchRunaways();

        for (Long userId : runawayUserIds) {
            taskExecutor.execute(() ->
                    notificationService.send(userId, "펫이 가출했습니다! 😱 빨리 돌아와주세요!")
            );
        }
    }

    // 3. [회원 삭제] 매일 새벽 4시: 탈퇴 후 90일 지난 계정 삭제
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeWithdrawnUsers() {
        log.info("🧹 [스케줄러] 탈퇴 회원 영구 삭제 작업 시작...");
        List<UserEntity> targets = userMapper.findWithdrawnUsersForPurge(); // 90일 지난 유저 조회

        int count = 0;
        for (UserEntity user : targets) {
            try {
                userMapper.deleteUserPermanently(user.getId());
                count++;
            } catch (Exception e) {
                log.error("영구 삭제 실패 (User: {}): {}", user.getId(), e.getMessage());
            }
        }
        log.info("✅ [스케줄러] 총 {}명의 탈퇴 회원 데이터가 영구 삭제되었습니다.", count);
    }
}