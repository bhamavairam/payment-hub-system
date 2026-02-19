package com.paymenthub.ms2.service;

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
    // Receives plain ISO 8583 JSON from Router
    //
    // PHASE 1 (now):  return mock SUCCESS immediately for testing
    // PHASE 2 (next): encrypt for NPCI standard + call Sarvatra
    // ─────────────────────────────────────────────────────────────
    @RabbitListener(
        queues = "${rabbitmq.queues.from-router}",
        concurrency = "10-50"
    )
    public void processTransaction(RabbitMessage message) {
        long start = System.currentTimeMillis();

        log.info("📨 MS2 received | correlationId={} | source={} | dest={}",
                message.getCorrelationId(),
                message.getSource(),
                message.getDestination());

        try {
            // Read the plain ISO JSON to log some fields
            @SuppressWarnings("unchecked")
            Map<String, String> isoFields = objectMapper.readValue(
                    message.getPlainJsonPayload(), Map.class);

            log.info("📋 ISO fields | MTI={} | Terminal={} | Amount={}",
                    isoFields.get("0"),   // Field 0  = MTI
                    isoFields.get("41"),  // Field 41 = Terminal ID
                    isoFields.get("4"));  // Field 4  = Amount

            // ── PHASE 1: Mock response ────────────────────────────────
            // TODO Phase 2: encrypt isoFields for NPCI
            //               call Sarvatra HTTP endpoint
            //               parse real NPCI response
            TransactionResponse response = buildMockSuccessResponse(
                    message.getCorrelationId());

            // Send response back to MS1
            rabbitTemplate.convertAndSend(
                    exchange, toMs1RoutingKey, response);

            log.info("✅ MS2 mock response sent in {}ms | correlationId={}",
                    System.currentTimeMillis() - start,
                    message.getCorrelationId());

        } catch (Exception e) {
            log.error("❌ MS2 processing error | correlationId={}",
                    message.getCorrelationId(), e);

            // Send error response back so MS1 does not hang
            TransactionResponse errorResponse = buildErrorResponse(
                    message.getCorrelationId(), e.getMessage());

            rabbitTemplate.convertAndSend(
                    exchange, toMs1RoutingKey, errorResponse);
        }
    }

    // ── Mock success response (PHASE 1 only) ─────────────────────
    private TransactionResponse buildMockSuccessResponse(String correlationId) {
        return TransactionResponse.builder()
                .correlationId(correlationId)
                .status("SUCCESS")
                .responseCode("00")            // 00 = Approved
                .responseMessage("Approved")
                .transactionId("NPCI-" + System.currentTimeMillis())
                .rrn("RRN" + System.currentTimeMillis())
                .approvalCode("AUTH" + (int)(Math.random() * 999999))
                .balance("000000000000")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // ── Error response so MS1 never hangs ────────────────────────
    private TransactionResponse buildErrorResponse(
            String correlationId, String errorMessage) {
        return TransactionResponse.builder()
                .correlationId(correlationId)
                .status("FAILED")
                .responseCode("96")            // 96 = System error
                .responseMessage("MS2 processing error: " + errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}