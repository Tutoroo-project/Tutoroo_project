package com.tutoroo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 비동기 처리(@Async) 활성화
public class AsyncConfig {

    /**
     * [기능: 비동기 스레드 풀 설정]
     * 설명: @Async가 붙은 메서드(로그 저장, TTS 캐싱 등)를 실행할 별도의 스레드 공장을 만듭니다.
     * 효과: 요청이 몰려도 스레드가 무한정 생성되어 서버가 다운되는 것을 방지합니다.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본적으로 대기하는 스레드 개수
        executor.setCorePoolSize(5);

        // 최대 스레드 개수 (동시 요청이 많을 때 늘어나는 한계)
        executor.setMaxPoolSize(20);

        // 대기열 크기 (모든 스레드가 바쁠 때 순서를 기다리는 요청 수)
        executor.setQueueCapacity(50);

        // 로그 식별용 접두사 (예: TutorAsync-1, TutorAsync-2 ...)
        executor.setThreadNamePrefix("TutorAsync-");

        executor.initialize();
        return executor;
    }
}