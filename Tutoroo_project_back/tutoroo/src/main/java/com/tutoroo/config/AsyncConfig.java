package com.tutoroo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * [기능: 비동기 스레드 풀 설정 - Java 21 Upgrade]
     * 변경: 기존 ThreadPoolTaskExecutor -> VirtualThreadTaskExecutor
     * 설명: 물리적인 스레드 개수 제한(CorePoolSize)을 없애고, 요청마다 가벼운 가상 스레드를 생성합니다.
     * 효과: I/O 대기(AI API 호출, DB 조회 등)가 많은 상황에서 처리량을 극대화합니다.
     */
    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        // Java 21의 가상 스레드 실행기를 Spring의 AsyncTaskExecutor로 래핑
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}