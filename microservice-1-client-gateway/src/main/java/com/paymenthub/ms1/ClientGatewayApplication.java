package com.paymenthub.ms1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ClientGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientGatewayApplication.class, args);
    }
}