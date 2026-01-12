package com.tutoroo.config;

import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final UserMapper userMapper;

    /**
     * [기능: 매일 밤 12시 랭킹 산정 배치]
     * 설명: 모든 유저를 포인트 순으로 조회하여 daily_rank 컬럼을 업데이트합니다.
     * 효과: 조회 시 계산 비용이 0이 되며, 100위 밖의 유저도 본인 등수를 알 수 있습니다.
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 매일 00시 00분 00초
    public void calculateDailyRankings() {
        log.info("일일 랭킹 산정 작업을 시작합니다.");

        // 1. 포인트 순으로 전체 유저 ID 조회 (메모리 절약을 위해 필요한 필드만 조회 권장)
        List<UserEntity> users = userMapper.findAllByOrderByTotalPointDesc();

        // 2. 순위 매기기 (Java Loop)
        // 10만 명 이하라면 이 방식이 가장 안전하고 DB 부하가 적습니다.
        int rank = 1;
        for (UserEntity user : users) {
            // 동점자 처리 로직이 필요하다면 여기서 추가 가능 (지금은 단순 순차 부여)
            userMapper.updateDailyRank(user.getId(), rank++);
        }

        log.info("총 {}명의 랭킹 업데이트 완료", users.size());
    }

    @Transactional
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthlyPoints() {
        log.info("월간 포인트 초기화 작업을 시작합니다.");
        try {
            userMapper.resetAllUserPoints();
            log.info("성공적으로 모든 사용자의 포인트가 초기화되었습니다.");
        } catch (Exception e) {
            log.error("포인트 초기화 중 오류 발생: {}", e.getMessage());
        }
    }
}