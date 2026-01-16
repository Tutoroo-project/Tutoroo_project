package com.tutoroo.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class FlywayFixConfig {

    @Bean
    @Profile("db-reset") // 현재 활성 프로필
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            // Flyway.clean() 대신 JDBC로 직접 테이블을 다 지워버립니다. (설정 무시)
            try (Connection con = flyway.getConfiguration().getDataSource().getConnection();
                 Statement stmt = con.createStatement()) {

                System.out.println("🔥 [Emergency] DB 강제 초기화 시작...");

                // 1. 외래키 제약 해제
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

                // 2. 현재 DB의 모든 테이블 조회
                List<String> tables = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()")) {
                    while (rs.next()) {
                        tables.add(rs.getString(1));
                    }
                }

                // 3. 테이블 삭제 (DROP TABLE)
                for (String table : tables) {
                    stmt.executeUpdate("DROP TABLE IF EXISTS " + table);
                    System.out.println("   - Deleted table: " + table);
                }

                // 4. 외래키 제약 복구
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                System.out.println("✅ [Emergency] DB 초기화 완료.");

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("DB 초기화 실패", e);
            }

            // 초기화된 DB에 새로운 스키마 적용
            flyway.migrate();
        };
    }
}