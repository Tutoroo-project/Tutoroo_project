package com.tutoroo.event;

import com.tutoroo.mapper.UserMapper;
import com.tutoroo.service.PetService;
import com.tutoroo.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetEventListener {

    private final UserMapper userMapper;
    private final PetService petService;
    private final RankingService rankingService; // [추가] 랭킹 서비스 주입

    @EventListener
    public void handleStudyCompleted(StudyCompletedEvent event) {
        Long userId = event.getUserId();
        int score = event.getScore();

        // 1. 유저 포인트 획득 (랭킹 + 지갑 모두 증가)
        // UserMapper XML에서 total_point = total_point + #{amount} 로직이 있다고 가정
        userMapper.earnPoints(userId, score);

        // [중요] 변경된 최신 포인트를 조회하여 Redis 랭킹에 반영
        // (단순 더하기보다 DB의 최종값을 가져와서 덮어쓰는 것이 안전함)
        try {
            var user = userMapper.findById(userId);
            if (user != null) {
                rankingService.updateUserScore(userId, user.getTotalPoint());
            }
        } catch (Exception e) {
            log.error("이벤트 처리 중 랭킹 갱신 실패: {}", e.getMessage());
        }

        log.info("학습 보상: 유저 포인트 +{} (랭킹 반영 완료)", score);

        // 2. 펫 경험치 지급 (60점 이상일 때 보너스)
        if (score >= 60) {
            int expGain = score / 2;
            log.info("학습 보상: 펫 경험치 +{}", expGain);
            petService.gainExp(userId, expGain);
        }
    }
}