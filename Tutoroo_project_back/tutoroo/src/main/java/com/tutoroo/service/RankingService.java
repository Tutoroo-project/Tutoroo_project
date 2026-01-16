package com.tutoroo.service;

import com.tutoroo.dto.RankingDTO;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;

    // Redis Key 상수
    private static final String LEADERBOARD_KEY = "leaderboard:total";

    /**
     * [기능: 실시간 랭킹 조회 (Redis ZSet 최적화)]
     * 설명: ZSet에서 Top 3와 전체 랭킹을 0.01초 내에 조회하여 반환합니다.
     */
    @Transactional(readOnly = true)
    public RankingDTO getRealtimeRankings() {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 1. 전체 랭킹 (1~100위) 조회 (Redis ZSet Reverse Range)
        // 반환값: Set<UserId(String)>
        Set<String> topUserIds = zSetOps.reverseRange(LEADERBOARD_KEY, 0, 99);

        if (topUserIds == null || topUserIds.isEmpty()) {
            return RankingDTO.builder()
                    .topRankers(Collections.emptyList())
                    .allRankers(Collections.emptyList())
                    .build();
        }

        // 2. 유저 상세 정보 조회 (DB)
        // Redis에는 ID와 점수만 있으므로, 프로필 정보는 DB에서 가져와야 함
        List<RankingDTO.RankEntry> allRankers = new ArrayList<>();
        int currentRank = 1;

        for (String userIdStr : topUserIds) {
            try {
                Long userId = Long.parseLong(userIdStr);
                UserEntity user = userMapper.findById(userId);

                if (user != null) {
                    // Redis 점수가 DB보다 더 최신일 수 있으므로 Redis 점수 사용
                    Double score = zSetOps.score(LEADERBOARD_KEY, userIdStr);
                    int totalPoint = (score != null) ? score.intValue() : user.getTotalPoint();

                    allRankers.add(RankingDTO.RankEntry.builder()
                            .rank(currentRank++)
                            .maskedName(user.getMaskedName())
                            .totalPoint(totalPoint)
                            .profileImage(user.getProfileImage())
                            .ageGroup(getAgeGroup(user.getAge()))
                            .build());
                }
            } catch (NumberFormatException e) {
                log.warn("랭킹 파싱 오류: {}", userIdStr);
            }
        }

        // 3. 상위 3명 추출
        List<RankingDTO.RankEntry> topRankers = new ArrayList<>();
        if (allRankers.size() >= 1) topRankers.add(allRankers.get(0));
        if (allRankers.size() >= 2) topRankers.add(allRankers.get(1));
        if (allRankers.size() >= 3) topRankers.add(allRankers.get(2));

        return RankingDTO.builder()
                .topRankers(topRankers)
                .allRankers(allRankers)
                .build();
    }

    /**
     * [기능: 랭킹 점수 업데이트]
     * 설명: 유저 점수가 변경될 때 Redis ZSet을 즉시 갱신합니다. (PetEventListener에서 호출)
     */
    public void updateUserScore(Long userId, int totalPoint) {
        try {
            redisTemplate.opsForZSet().add(LEADERBOARD_KEY, String.valueOf(userId), totalPoint);
            log.info("🏆 랭킹 업데이트 완료: UserID={} Point={}", userId, totalPoint);
        } catch (Exception e) {
            log.error("랭킹 업데이트 실패: {}", e.getMessage());
        }
    }

    /**
     * [기능: 필터링 랭킹 조회]
     * 설명: 성별/연령별 필터링은 경우의 수가 많아 DB 쿼리를 사용하되, 짧게 캐싱할 수도 있습니다.
     * 여기서는 복잡도를 낮추기 위해 DB 실시간 조회를 수행합니다.
     */
    @Transactional(readOnly = true)
    public RankingDTO getFilteredRankings(RankingDTO.FilterRequest filter) {
        List<UserEntity> users = userMapper.getRankingList(filter.gender(), filter.ageGroup());

        List<RankingDTO.RankEntry> rankEntries = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            UserEntity u = users.get(i);
            rankEntries.add(RankingDTO.RankEntry.builder()
                    .rank(i + 1)
                    .maskedName(u.getMaskedName())
                    .totalPoint(u.getTotalPoint())
                    .profileImage(u.getProfileImage())
                    .ageGroup(getAgeGroup(u.getAge()))
                    .build());
        }

        return RankingDTO.builder()
                .topRankers(rankEntries.stream().limit(3).toList())
                .allRankers(rankEntries)
                .build();
    }

    // 헬퍼 메서드: 연령대 계산
    private String getAgeGroup(Integer age) {
        if (age == null) return "알수없음";
        int group = (age / 10) * 10;
        return group + "대";
    }
}