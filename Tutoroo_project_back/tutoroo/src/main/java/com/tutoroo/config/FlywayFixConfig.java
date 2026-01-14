package com.tutoroo.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FlywayFixConfig {

    /**
     * [기능: DB 초기화 방지 및 안전한 마이그레이션]
     * 수정: 기존 flyway.clean() 호출을 제거하여 데이터 삭제 사고를 방지합니다.
     * 설명: 서버가 시작될 때 기존 데이터는 유지하고, 변경된 스키마(V1__...sql)만 반영합니다.
     * * 주의: 만약 개발 초기 단계라 매번 DB를 밀어야 한다면
     * @Profile("local") 등을 붙여 로컬에서만 동작하게 하거나 수동으로 clean 하십시오.
     */
    @Bean
    public FlywayMigrationStrategy safeMigrationStrategy() {
        return flyway -> {
            // [위험] flyway.clean(); 코드는 삭제되었습니다.

            // 변경사항이 생기면 자동으로 repair 후 migrate 진행
            flyway.repair();
            flyway.migrate();
        };
    }
}