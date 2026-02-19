package com.paymenthub.ms3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.common.dto.RabbitMessage;
import com.paymenthub.common.dto.TransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class TransactionProcessorService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.to-ms1}")
    private String toMs1RoutingKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    // MS3 handles VISA / MASTERCARD destinations
    // PHASE 1: returns mock SUCCESS
    // PHASE 2: will call VISA/MC network
    // ─────────────────────────────────────────────────────────────
    @RabbitListener(
        queues = "${rabbitmq.queues.from-router}",
        concurrency = "10-50"
    )
    public void processTransaction(RabbitMessage message) {
        long start = System.currentTimeMillis();

        log.info("📨 MS3 received | correlationId={} | source={} | dest={}",
                message.getCorrelationId(),
                message.getSource(),
                message.getDestination());

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> isoFields = objectMapper.readValue(
                    message.getPlainJsonPayload(), Map.class);

            log.info("📋 ISO fields | MTI={} | Terminal={} | Amount={}",
                    isoFields.get("0"),
                    isoFields.get("41"),
                    isoFields.get("4"));

            // ── PHASE 1: Mock response ────────────────────────────
            TransactionResponse response = buildMockSuccessResponse(
                    message.getCorrelationId());

            rabbitTemplate.convertAndSend(
                    exchange, toMs1RoutingKey, response);

            log.info("✅ MS3 mock response sent in {}ms | correlationId={}",
                    System.currentTimeMillis() - start,
                    message.getCorrelationId());

        } catch (Exception e) {
            log.error("❌ MS3 error | correlationId={}",
                    message.getCorrelationId(), e);

            TransactionResponse errorResponse = buildErrorResponse(
                    message.getCorrelationId(), e.getMessage());

            rabbitTemplate.convertAndSend(
                    exchange, toMs1RoutingKey, errorResponse);
        }
    }

    private TransactionResponse buildMockSuccessResponse(String correlationId) {
        return TransactionResponse.builder()
                .correlationId(correlationId)
                .status("SUCCESS")
                .responseCode("00")
                .responseMessage("Approved")
                .transactionId("MS3-" + System.currentTimeMillis())
                .rrn("RRN" + System.currentTimeMillis())
                .approvalCode("AUTH" + (int)(Math.random() * 999999))
                .balance("000000000000")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransactionResponse buildErrorResponse(
            String correlationId, String errorMessage) {
        return TransactionResponse.builder()
                .correlationId(correlationId)
                .status("FAILED")
                .responseCode("96")
                .responseMessage("MS3 error: " + errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}