package com.popIt.pop_it.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    // 공간 등록 응답을 임베딩(Gemini API 호출) 완료까지 기다리지 않도록 별도 스레드 풀에서 처리
    @Bean
    public Executor embeddingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("embedding-");
        executor.initialize();
        return executor;
    }

    // 유저 취향 벡터 재계산 디바운스 전용
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService userVectorDebounceScheduler() {
        return Executors.newScheduledThreadPool(2, new CustomizableThreadFactory("vector-debounce-"));
    }
}
