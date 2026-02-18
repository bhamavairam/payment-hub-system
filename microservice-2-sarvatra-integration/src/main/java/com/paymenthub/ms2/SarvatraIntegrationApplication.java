package com.paymenthub.ms2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class SarvatraIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SarvatraIntegrationApplication.class, args);
    }
}