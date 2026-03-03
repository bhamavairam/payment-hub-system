package com.paymenthub.npci.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

@Service
@Slf4j
@RequiredArgsConstructor
public class NpciListenerService implements InitializingBean {

    private final NpciProcessingService processingService;

    @Override
    public void afterPropertiesSet() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("🏦 NPCI LISTENER SERVICE INITIALIZED");
        log.info("═══════════════════════════════════════════════════════");
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(
            queues = "${rabbitmq.queues.npci-input}", concurrency = "10-30")
    public void receive(com.paymenthub.common.dto.CanonicalMessage message) {
        log.info("═══════════════════════════════════════════════════════");
        log.info("🏦 NPCI RECEIVED MESSAGE");
        log.info("═══════════════════════════════════════════════════════");
        log.info("   CorrelationId: {}", message.getCorrelationId());
        log.info("   TraceId: {}", message.getTraceId());
        log.info("   Gateway: {}", message.getTargetGateway());
        log.info("   Amount: {}", message.getAmount());
        log.info("═══════════════════════════════════════════════════════");

        processingService.process(message);
    }
}