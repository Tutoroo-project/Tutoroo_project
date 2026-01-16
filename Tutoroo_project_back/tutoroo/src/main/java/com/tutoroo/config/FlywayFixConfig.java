package com.tutoroo.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FlywayFixConfig {

    @Bean
    @Profile("secret") // [중요] 개발용 프로필('secret')에서만 동작하게 안전장치 걸기
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            // 1. 기존 데이터베이스를 싹 지웁니다 (초기화)
            flyway.clean();
            // 2. V1 파일부터 다시 실행해서 테이블을 예쁘게 새로 만듭니다.
            flyway.migrate();
        };
    }
}