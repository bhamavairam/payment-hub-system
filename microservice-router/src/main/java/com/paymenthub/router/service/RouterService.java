package com.paymenthub.router.service;

import com.paymenthub.common.dto.CanonicalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RouterService {

    @Autowired
    private QueuePublisherService queuePublisher;

    /**
     * Main routing logic - listens to router.queue
     * NON-BLOCKING - Fire and Forget Architecture
     */
    @RabbitListener(
            queues = "${rabbitmq.queues.router-input}",
            concurrency = "10-50")
    public void routeMessage(CanonicalMessage message) {

        long startTime = System.nanoTime();

        try {

            String correlationId = message.getCorrelationId();
            String gateway = message.getTargetGateway();
            String txnType = message.getTxnType();
            Boolean enableFraud = message.getEnableFraud();
            Boolean enableNotification = message.getEnableNotification();

            log.info("═══════════════════════════════════════════════════════");
            log.info("📨 ROUTING MESSAGE | {}", correlationId);
            log.info("═══════════════════════════════════════════════════════");

            log.info("📋 Message details:");
            log.info("   Trace: {}", message.getTraceId());
            log.info("   Gateway: {}", gateway);
            log.info("   Card Network: {}", message.getCardNetwork());
            log.info("   Type: {}", txnType);
            log.info("   Amount: {}", message.getAmount());
            log.info("   🔍 Fraud Check: {}", enableFraud);
            log.info("   📧 Notification: {}", enableNotification);

            // ════════════════════════════════════════════════════
            // FIRE AND FORGET PUBLISHING (NO WAITING)
            // ════════════════════════════════════════════════════

            // 1️⃣ ALWAYS send to transform queue
            queuePublisher.publishToTransformQueue(message, gateway);

            // 2️⃣ CONDITIONAL FRAUD
            if (Boolean.TRUE.equals(enableFraud)) {
                queuePublisher.publishToFraudQueue(message);
            } else {
                log.info("⏭️  Fraud check DISABLED for this transaction");
            }

            // 3️⃣ CONDITIONAL NOTIFICATION
            if (Boolean.TRUE.equals(enableNotification)) {
                queuePublisher.publishToNotificationQueue(message);
            } else {
                log.info("⏭️  Notification DISABLED for this transaction");
            }

            long totalTime = (System.nanoTime() - startTime) / 1_000_000;

            log.info("═══════════════════════════════════════════════════════");
            log.info("✅ ROUTING SUBMITTED - {}ms", totalTime);
            log.info("   Transform: {} ✓", gateway);
            log.info("   Fraud: {}", Boolean.TRUE.equals(enableFraud) ? "✓" : "SKIPPED");
            log.info("   Notification: {}", Boolean.TRUE.equals(enableNotification) ? "✓" : "SKIPPED");
            log.info("═══════════════════════════════════════════════════════");

        } catch (Exception e) {

            long totalTime = (System.nanoTime() - startTime) / 1_000_000;

            log.error("═══════════════════════════════════════════════════════");
            log.error("❌ ROUTING FAILED - {}ms", totalTime, e);
            log.error("═══════════════════════════════════════════════════════");
        }
    }
}