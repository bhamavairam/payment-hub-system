package com.paymenthub.router.service;

import com.paymenthub.common.dto.CanonicalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class QueuePublisherService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.transform-npci}")
    private String transformNpciKey;

    @Value("${rabbitmq.routing-keys.transform-visa}")
    private String transformVisaKey;

    @Value("${rabbitmq.routing-keys.transform-mastercard}")
    private String transformMastercardKey;

    @Value("${rabbitmq.routing-keys.fraud}")
    private String fraudKey;

    @Value("${rabbitmq.routing-keys.notification}")
    private String notificationKey;

    /**
     * Publish to transform queue based on gateway
     */
    @Async("routerExecutor")
    public CompletableFuture<Void> publishToTransformQueue(
            CanonicalMessage message, String gateway) {
        
        long start = System.nanoTime();
        
        try {
            String routingKey = getTransformRoutingKey(gateway);
            
            if (routingKey == null) {
                log.warn("⚠️ Unknown gateway: {} | Skipping transform queue", gateway);
                return CompletableFuture.completedFuture(null);
            }

            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            
            long duration = (System.nanoTime() - start) / 1_000_000;
            log.info("📤 Transform queue | {} → {} ({}ms)", 
                    message.getCorrelationId(), gateway, duration);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ Failed to publish to transform queue | {} | {}", 
                    message.getCorrelationId(), gateway, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Publish to fraud queue (always)
     */
    @Async("routerExecutor")
    public CompletableFuture<Void> publishToFraudQueue(CanonicalMessage message) {
        
        long start = System.nanoTime();
        
        try {
            rabbitTemplate.convertAndSend(exchange, fraudKey, message);
            
            long duration = (System.nanoTime() - start) / 1_000_000;
            log.info("📤 Fraud queue | {} ({}ms)", 
                    message.getCorrelationId(), duration);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ Failed to publish to fraud queue | {}", 
                    message.getCorrelationId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Publish to notification queue (always)
     */
    @Async("routerExecutor")
    public CompletableFuture<Void> publishToNotificationQueue(
            CanonicalMessage message) {
        
        long start = System.nanoTime();
        
        try {
            rabbitTemplate.convertAndSend(exchange, notificationKey, message);
            
            long duration = (System.nanoTime() - start) / 1_000_000;
            log.info("📤 Notification queue | {} ({}ms)", 
                    message.getCorrelationId(), duration);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ Failed to publish to notification queue | {}", 
                    message.getCorrelationId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Get routing key based on gateway
     */
    private String getTransformRoutingKey(String gateway) {
        if (gateway == null) {
            return null;
        }

        switch (gateway.toUpperCase()) {
            case "NPCI":
            case "RUPAY":
                return transformNpciKey;
            
            case "VISA":
                return transformVisaKey;
            
            case "MASTERCARD":
            case "MC":
                return transformMastercardKey;
            
            default:
                return null;
        }
    }
}