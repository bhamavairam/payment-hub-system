package com.paymenthub.router.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "routerExecutor")
    public Executor routerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);      // Handle 50 concurrent routes
        executor.setMaxPoolSize(200);      // Max 200 threads
        executor.setQueueCapacity(1000);   // Queue up to 1000 messages
        executor.setThreadNamePrefix("router-");
        executor.initialize();
        return executor;
    }
}