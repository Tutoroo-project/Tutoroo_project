package com.tutoroo.service;

import com.tutoroo.dto.RankingDTO;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * [기능: 랭킹 서비스]
 * 설명: Redis(실시간 전체 랭킹)와 DB(필터링 랭킹)를 하이브리드로 사용하여 성능과 기능을 모두 잡았습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserMapper userMapper;

    // Redis Key
    private static final String RANKING_KEY = "user:ranking";

    /**
     * [기능 1] 점수 갱신 (Redis)
     * 설명: 유저 점수가 변동되면 Redis ZSet에 반영합니다.
     */
    public void updateUserScore(Long userId, int totalPoint) {
        redisTemplate.opsForZSet().add(RANKING_KEY, String.valueOf(userId), totalPoint);
        log.info("🏆 Redis 랭킹 갱신 - User: {}, Point: {}", userId, totalPoint);
    }

    /**
     * [기능 2] 실시간 전체 랭킹 조회 (Redis -> DB)
     * 설명: Redis에서 100명을 빠르게 가져온 뒤, 상세 정보(이름, 사진 등)는 DB에서 채웁니다.
     */
    public RankingDTO getRealtimeRankings() {
        // 1. Redis에서 점수 높은 순(Reverse)으로 0~99등 조회
        Set<ZSetOperations.TypedTuple<String>> topRankers =
                redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, 99);

        if (topRankers == null || topRankers.isEmpty()) {
            return RankingDTO.builder()
                    .topRankers(List.of())
                    .allRankers(List.of())
                    .build();
        }

        List<RankingDTO.RankEntry> entries = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : topRankers) {
            String userIdStr = tuple.getValue();
            Double score = tuple.getScore();
            Long userId = Long.parseLong(userIdStr);

            // 유저 상세 정보 조회 (캐싱 권장 구간)
            UserEntity user = userMapper.findById(userId);

            // DTO 매핑
            entries.add(RankingDTO.RankEntry.builder()
                    .rank(rank++)
                    .maskedName(maskName(user != null ? user.getName() : "알수없음"))
                    .totalPoint(score != null ? score.intValue() : 0)
                    .profileImage(user != null ? user.getProfileImage() : null) // 추가된 필드
                    .ageGroup(user != null ? convertAgeGroup(user.getAge()) : "") // 추가된 필드
                    .build());
        }

        return RankingDTO.builder()
                .topRankers(entries.stream().limit(3).collect(Collectors.toList()))
                .allRankers(entries)
                .build();
    }

    /**
     * [기능 3] 필터링 랭킹 조회 (DB)
     * 설명: 성별, 나이대 같은 복잡한 조건은 DB 쿼리로 조회합니다. (보내주신 코드 보완)
     */
    public RankingDTO getFilteredRankings(RankingDTO.FilterRequest filter) {
        // DB에서 필터링된 목록 조회
        List<UserEntity> users = userMapper.getRankingList(filter.getGender(), filter.getAgeGroup());

        List<RankingDTO.RankEntry> entries = IntStream.range(0, users.size())
                .mapToObj(i -> {
                    UserEntity user = users.get(i);
                    return RankingDTO.RankEntry.builder()
                            .rank(i + 1)
                            .maskedName(maskName(user.getName())) // 마스킹 적용
                            .totalPoint(user.getTotalPoint())
                            .profileImage(user.getProfileImage()) // 추가된 필드
                            .ageGroup(convertAgeGroup(user.getAge())) // 추가된 필드
                            .build();
                })
                .collect(Collectors.toList());

        return RankingDTO.builder()
                .topRankers(entries.stream().limit(3).collect(Collectors.toList()))
                .allRankers(entries)
                .build();
    }

    // --- Helper Methods ---

    // 이름 마스킹 (홍길동 -> 홍*동)
    private String maskName(String name) {
        if (name == null || name.length() < 2) return name;
        return name.charAt(0) + "*" + name.substring(2);
        // 혹은 중간 글자만 가리기: name.charAt(0) + "*".repeat(name.length()-1)
    }

    // 나이 -> 연령대 변환 (18 -> "10대")
    private String convertAgeGroup(Integer age) {
        if (age == null) return "";
        return (age / 10 * 10) + "대";
    }
}