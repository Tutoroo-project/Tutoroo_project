package com.tutoroo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
@MapperScan("com.tutoroo.mapper") // 인터페이스 매퍼 위치 지정
public class MyBatisConfig {

    /**
     * [기능: MyBatis 세부 설정 커스터마이징]
     * 작동원리:
     * 1. mapUnderscoreToCamelCase를 활성화하여 DB 컬럼명과 Java 필드명을 자동 매칭합니다.
     * 2. null 값이 들어올 때의 처리 방식을 정의합니다.
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            configuration.setMapUnderscoreToCamelCase(true); // user_name -> userName
            configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);
            configuration.setDefaultFetchSize(100); // 대량 데이터 조회 시 성능 최적화
        };
    }
}