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

@Slf4j
//@Component
@Profile("local") // 로컬 환경에서만 실행되도록 제한
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserMapper userMapper;
    private final RankingService rankingService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userMapper.countAllUsers() > 0) {
            log.info("데이터가 이미 존재하므로 초기화 작업을 건너뜁니다.");
            return;
        }

        log.info("초기 테스트 데이터 생성을 시작합니다.");
        Random random = new Random();

        for (int i = 1; i <= 50; i++) {
            String username = "user" + i;
            String name = "Student" + i;

            int randomPoint = random.nextInt(5001);
            int randomAge = 14 + random.nextInt(6);
            String gender = (i % 2 == 0) ? "MALE" : "FEMALE";

            UserEntity user = UserEntity.builder()
                    .username(username)
                    .password("{noop}1234") // 테스트용 비암호화 패스워드
                    .name(name)
                    .email(username + "@test.com")
                    .role("ROLE_USER")
                    .membershipTier(MembershipTier.BASIC)
                    .totalPoint(randomPoint)
                    .age(randomAge)
                    .gender(gender)
                    .build();

            userMapper.save(user);

            // Redis 랭킹 동기화
            rankingService.updateUserScore(user.getId(), user.getTotalPoint());
        }

        log.info("테스트 유저 50명 생성 및 랭킹 등록 완료.");
    }
}