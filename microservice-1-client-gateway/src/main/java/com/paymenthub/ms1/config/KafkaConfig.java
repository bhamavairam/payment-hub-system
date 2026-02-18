package com.paymenthub.ms1.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.to-ms2}")
    private String toMs2Topic;

    @Bean
    public NewTopic toMs2Topic() {
        return TopicBuilder.name(toMs2Topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}