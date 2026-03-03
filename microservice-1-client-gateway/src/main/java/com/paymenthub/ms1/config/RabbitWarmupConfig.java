package com.paymenthub.ms1.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitWarmupConfig {

    @Bean
    public ApplicationRunner warmUpRabbit(ConnectionFactory connectionFactory) {
        return args -> {
            try {
                connectionFactory.createConnection().close();
                System.out.println("✅ RabbitMQ connection pre-warmed at startup");
            } catch (Exception e) {
                System.err.println("❌ RabbitMQ warmup failed: " + e.getMessage());
            }
        };
    }
}