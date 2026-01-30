package com.tutoroo.service;

import com.tutoroo.dto.RankingDTO;
import com.tutoroo.dto.RivalDTO;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;

    // Redis Key (전체 랭킹)
    private static final String LEADERBOARD_KEY = "leaderboard:total";

    /**
     * [기능: 실시간 랭킹 조회 (Redis ZSet 최적화)]
     * 개선점: 기존 N+1 문제를 reverseRangeWithScores로 해결하여 Redis 부하를 1/100로 줄임.
     */
    @Transactional(readOnly = true)
    public RankingDTO getRealtimeRankings(Long myUserId) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 1. [최적화] ID와 점수를 한 번에 조회 (Tuple 사용)
        Set<ZSetOperations.TypedTuple<String>> topRankersTuple = zSetOps.reverseRangeWithScores(LEADERBOARD_KEY, 0, 99);

        if (topRankersTuple == null || topRankersTuple.isEmpty()) {
            return new RankingDTO(Collections.emptyList(), Collections.emptyList(), null);
        }

        List<RankingDTO.RankEntry> allRankers = new ArrayList<>();
        int currentRank = 1;

        // 2. DTO 변환
        for (ZSetOperations.TypedTuple<String> tuple : topRankersTuple) {
            String userIdStr = tuple.getValue();
            Double score = tuple.getScore(); // Redis 점수 사용 (DB 조회 불필요)

            try {
                Long userId = Long.parseLong(userIdStr);
                // 유저 정보는 DB에서 조회 (캐싱 적용 권장)
                UserEntity user = userMapper.findById(userId);

                if (user != null) {
                    allRankers.add(RankingDTO.RankEntry.builder()
                            .rank(currentRank++)
                            .maskedName(user.getMaskedName())
                            .totalPoint(score != null ? score.intValue() : 0)
                            .profileImage(user.getProfileImage())
                            .ageGroup(getAgeGroup(user.getAge()))
                            .build());
                }
            } catch (NumberFormatException e) {
                log.warn("랭킹 데이터 파싱 오류: {}", userIdStr);
            }
        }

        // 3. Top 3 및 내 랭킹 추출
        List<RankingDTO.RankEntry> top3 = allRankers.stream().limit(3).toList();
        RankingDTO.RankEntry myRankEntry = (myUserId != null) ? getMyRealtimeRank(myUserId, zSetOps) : null;

        return new RankingDTO(top3, allRankers, myRankEntry);
    }

    /**
     * [기능: 필터링 랭킹 조회]
     * 설명: 성별/연령별 랭킹 리스트를 조회하고, 그 안에서 내 순위를 찾습니다.
     */
    @Transactional(readOnly = true)
    public RankingDTO getFilteredRankings(RankingDTO.FilterRequest filter, Long myUserId) {
        // DB 쿼리 (이미 점수순 정렬되어 옴)
        List<UserEntity> users = userMapper.getRankingList(filter.gender(), filter.ageGroup());

        List<RankingDTO.RankEntry> rankEntries = new ArrayList<>();
        RankingDTO.RankEntry myRankEntry = null;

        for (int i = 0; i < users.size(); i++) {
            UserEntity u = users.get(i);
            int rank = i + 1;

            RankingDTO.RankEntry entry = RankingDTO.RankEntry.builder()
                    .rank(rank)
                    .maskedName(u.getMaskedName())
                    .totalPoint(u.getTotalPoint())
                    .profileImage(u.getProfileImage())
                    .ageGroup(getAgeGroup(u.getAge()))
                    .build();

            rankEntries.add(entry);

            if (myUserId != null && u.getId().equals(myUserId)) {
                myRankEntry = entry;
            }
        }

        return new RankingDTO(
                rankEntries.stream().limit(3).toList(),
                rankEntries,
                myRankEntry
        );
    }

    /**
     * [기능: 라이벌 정보 비교 (Step 17 누락 기능 추가)]
     */
    @Transactional(readOnly = true)
    public RivalDTO.RivalComparisonResponse compareRival(Long myUserId) {
        UserEntity me = userMapper.findById(myUserId);

        // 라이벌이 없는 경우
        if (me.getRivalId() == null) {
            return RivalDTO.RivalComparisonResponse.builder()
                    .hasRival(false)
                    .myProfile(toRivalProfile(me))
                    .message("아직 라이벌이 없습니다. 매칭을 시작해보세요!")
                    .build();
        }

        // 라이벌 정보 조회
        UserEntity rival = userMapper.findById(me.getRivalId());
        if (rival == null) {
            // 예외 처리: 라이벌 계정이 삭제된 경우 등
            return RivalDTO.RivalComparisonResponse.builder()
                    .hasRival(false)
                    .myProfile(toRivalProfile(me))
                    .message("라이벌 정보를 찾을 수 없습니다.")
                    .build();
        }

        int gap = me.getTotalPoint() - rival.getTotalPoint();
        String message = gap > 0
                ? "라이벌을 " + gap + "점 앞서고 있어요! 😎"
                : "분발하세요! 라이벌이 " + Math.abs(gap) + "점 앞서갑니다. 🔥";

        return RivalDTO.RivalComparisonResponse.builder()
                .hasRival(true)
                .myProfile(toRivalProfile(me))
                .rivalProfile(toRivalProfile(rival))
                .message(message)
                .pointGap(Math.abs(gap))
                .build();
    }

    /**
     * [기능: 랭킹 점수 업데이트]
     */
    public void updateUserScore(Long userId, int totalPoint) {
        try {
            redisTemplate.opsForZSet().add(LEADERBOARD_KEY, String.valueOf(userId), totalPoint);
        } catch (Exception e) {
            log.error("랭킹 업데이트 실패: {}", e.getMessage());
        }
    }

    // --- Helper Methods ---

    private RankingDTO.RankEntry getMyRealtimeRank(Long myUserId, ZSetOperations<String, String> zSetOps) {
        try {
            String userIdStr = String.valueOf(myUserId);
            Long rankIndex = zSetOps.reverseRank(LEADERBOARD_KEY, userIdStr);
            Double score = zSetOps.score(LEADERBOARD_KEY, userIdStr);

            if (rankIndex != null && score != null) {
                UserEntity me = userMapper.findById(myUserId);
                if (me != null) {
                    return RankingDTO.RankEntry.builder()
                            .rank(rankIndex.intValue() + 1)
                            .maskedName(me.getMaskedName())
                            .totalPoint(score.intValue())
                            .profileImage(me.getProfileImage())
                            .ageGroup(getAgeGroup(me.getAge()))
                            .build();
                }
            }
        } catch (Exception e) { /* 무시 */ }
        return null;
    }

    private RivalDTO.RivalProfile toRivalProfile(UserEntity user) {
        return RivalDTO.RivalProfile.builder()
                .userId(user.getId())
                .name(user.getMaskedName())
                .profileImage(user.getProfileImage())
                .totalPoint(user.getTotalPoint())
                .tier(user.getEffectiveTier().name())
                .level(user.getLevel())
                .build();
    }

    private String getAgeGroup(Integer age) {
        if (age == null) return "알수없음";
        return (age / 10 * 10) + "대";
    }
}