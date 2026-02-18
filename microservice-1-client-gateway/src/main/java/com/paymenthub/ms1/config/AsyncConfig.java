package com.paymenthub.ms1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // Thread pool for parallel DB save + RabbitMQ send
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);     // Always ready threads
        executor.setMaxPoolSize(200);     // Max threads under load
        executor.setQueueCapacity(1000);  // Queue if all busy
        executor.setThreadNamePrefix("parallel-");
        executor.initialize();
        return executor;
    }
}