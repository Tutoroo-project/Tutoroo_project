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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
     * 개선점: 기존 N+1 문제를 findAllByIds로 해결하여 Redis 부하를 1/100로 줄임.
     */
    @Transactional(readOnly = true)
    public RankingDTO getRealtimeRankings(Long myUserId) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 1. Redis에서 Top 100 ID 조회 (점수 포함)
        Set<ZSetOperations.TypedTuple<String>> topRankersWithScore =
                zSetOps.reverseRangeWithScores(LEADERBOARD_KEY, 0, 99);

        if (topRankersWithScore == null || topRankersWithScore.isEmpty()) {
            return new RankingDTO(Collections.emptyList(), Collections.emptyList(), null);
        }

        // 2. ID 리스트 추출
        List<Long> userIds = topRankersWithScore.stream()
                .map(tuple -> Long.parseLong(tuple.getValue()))
                .toList();

        // 3. [최적화] DB에서 한 번에 조회 (WHERE IN) -> Map으로 변환하여 O(1) 접근
        List<UserEntity> users = userMapper.findAllByIds(userIds);
        Map<Long, UserEntity> userMap = users.stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        // 4. Redis 순서대로 DTO 조립
        List<RankingDTO.RankEntry> allRankers = new ArrayList<>();
        int currentRank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : topRankersWithScore) {
            Long uid = Long.parseLong(tuple.getValue());
            UserEntity user = userMap.get(uid);
            int score = tuple.getScore() != null ? tuple.getScore().intValue() : 0;

            if (user != null) {
                allRankers.add(RankingDTO.RankEntry.builder()
                        .rank(currentRank++)
                        .maskedName(user.getMaskedName())
                        .totalPoint(score) // Redis 점수가 최신
                        .profileImage(user.getProfileImage())
                        .ageGroup(getAgeGroup(user.getAge()))
                        .build());
            }
        }

        // 5. 상위 3명 추출
        List<RankingDTO.RankEntry> topRankers = allRankers.stream()
                .limit(3)
                .toList();

        // 6. 내 랭킹 조회 (로그인 시)
        RankingDTO.RankEntry myRank = null;
        if (myUserId != null) {
            myRank = getMyRank(myUserId, zSetOps);
        }

        return new RankingDTO(topRankers, allRankers, myRank);
    }

    /**
     * [기능: 필터링 랭킹 조회 (DB 조회)]
     * 참고: 필터링(성별, 연령)은 Redis ZSet으로 구현하기 복잡하므로 DB 쿼리를 사용합니다.
     */
    @Transactional(readOnly = true)
    public RankingDTO getFilteredRankings(RankingDTO.FilterRequest filter, Long myUserId) {
        List<UserEntity> entities = userMapper.getRankingList(filter.gender(), filter.ageGroup());

        List<RankingDTO.RankEntry> allRankers = new ArrayList<>();
        RankingDTO.RankEntry myRank = null;

        for (int i = 0; i < entities.size(); i++) {
            UserEntity user = entities.get(i);
            RankingDTO.RankEntry entry = RankingDTO.RankEntry.builder()
                    .rank(i + 1)
                    .maskedName(user.getMaskedName())
                    .totalPoint(user.getTotalPoint())
                    .profileImage(user.getProfileImage())
                    .ageGroup(getAgeGroup(user.getAge()))
                    .build();

            allRankers.add(entry);

            if (myUserId != null && user.getId().equals(myUserId)) {
                myRank = entry;
            }
        }

        List<RankingDTO.RankEntry> topRankers = allRankers.stream().limit(3).toList();
        return new RankingDTO(topRankers, allRankers, myRank);
    }

    /**
     * [기능: 라이벌 비교]
     */
    @Transactional(readOnly = true)
    public RivalDTO.RivalComparisonResponse compareRival(Long myUserId) {
        UserEntity me = userMapper.findById(myUserId);
        if (me == null) throw new RuntimeException("유저 정보를 찾을 수 없습니다.");

        if (me.getRivalId() == null) {
            return RivalDTO.RivalComparisonResponse.builder()
                    .hasRival(false)
                    .myProfile(toRivalProfile(me))
                    .message("아직 라이벌이 없습니다. 매칭을 시작해보세요!")
                    .pointGap(0)
                    .build();
        }

        UserEntity rival = userMapper.findById(me.getRivalId());
        // 라이벌이 탈퇴했을 경우 방어 로직
        if (rival == null) {
            return RivalDTO.RivalComparisonResponse.builder()
                    .hasRival(false)
                    .myProfile(toRivalProfile(me))
                    .message("라이벌이 떠났습니다. 새로운 라이벌을 찾아보세요.")
                    .pointGap(0)
                    .build();
        }

        int gap = me.getTotalPoint() - rival.getTotalPoint();
        String msg;
        if (gap > 0) msg = "라이벌보다 " + gap + "점 앞서고 있어요! 😆";
        else if (gap < 0) msg = "라이벌에게 " + Math.abs(gap) + "점 뒤쳐지고 있어요. 분발하세요! 🔥";
        else msg = "라이벌과 점수가 똑같아요! 긴장감이 넘치네요. ⚡";

        return RivalDTO.RivalComparisonResponse.builder()
                .hasRival(true)
                .myProfile(toRivalProfile(me))
                .rivalProfile(toRivalProfile(rival))
                .message(msg)
                .pointGap(Math.abs(gap))
                .build();
    }

    // --- Helper Methods ---

    // Redis 점수 갱신 (UserEventListener 등에서 호출)
    public void updateUserScore(Long userId, int totalPoint) {
        redisTemplate.opsForZSet().add(LEADERBOARD_KEY, String.valueOf(userId), totalPoint);
    }

    private RankingDTO.RankEntry getMyRank(Long myUserId, ZSetOperations<String, String> zSetOps) {
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