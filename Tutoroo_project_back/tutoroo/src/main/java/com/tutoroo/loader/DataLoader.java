package com.tutoroo.loader;

import com.tutoroo.entity.MembershipTier;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

/**
 * [기능: 초기 더미 데이터 생성기]
 * 설명: 서버 시작 시 유저 정보가 하나도 없다면, 테스트용 유저 50명을 자동으로 생성합니다.
 * (DB 저장 + Redis 랭킹 등록 동시에 수행)
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserMapper userMapper;
    private final RankingService rankingService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. 이미 데이터가 있는지 확인 (있으면 생성 안 함)
        if (userMapper.countAllUsers() > 0) {
            log.info("✅ 데이터가 이미 존재합니다. 더미 데이터 생성을 건너뜁니다.");
            return;
        }

        log.info("🚀 테스트용 더미 유저 50명 생성을 시작합니다...");
        Random random = new Random();

        // 2. 유저 50명 생성 루프
        for (int i = 1; i <= 50; i++) {
            String username = "user" + i;
            String name = "학생" + i + "호";

            // 랜덤 점수 (0 ~ 5000점)
            int randomPoint = random.nextInt(5001);

            // 랜덤 나이 (14 ~ 19세)
            int randomAge = 14 + random.nextInt(6);

            // 랜덤 성별 (MALE / FEMALE)
            String gender = (i % 2 == 0) ? "MALE" : "FEMALE";

            UserEntity user = UserEntity.builder()
                    .username(username)
                    .password("{noop}1234") // 패스워드 암호화 없이 테스트용
                    .name(name)
                    .email(username + "@test.com")
                    .role("ROLE_USER")
                    .membershipTier(MembershipTier.BASIC)
                    .totalPoint(randomPoint) // 점수 부여
                    .age(randomAge)
                    .gender(gender)
                    .profileImage(null)
                    .build();

            // 3. MySQL 저장
            userMapper.save(user); // save 후 user.getId()에 PK가 담겨야 함

            // 4. Redis 랭킹 등록 (핵심!)
            // 방금 만든 유저의 ID와 점수를 Redis에 바로 꽂아줍니다.
            rankingService.updateUserScore(user.getId(), randomPoint);
        }

        log.info("🎉 더미 유저 50명 생성 및 Redis 랭킹 등록 완료!");
    }
}