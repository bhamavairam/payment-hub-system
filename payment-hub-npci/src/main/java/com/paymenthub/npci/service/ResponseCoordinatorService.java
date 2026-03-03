package com.paymenthub.npci.service;

import com.paymenthub.common.dto.CanonicalMessage;
import com.paymenthub.common.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResponseCoordinatorService {

    private final RabbitTemplate rabbitTemplate;

    // Use exchange + routing key to send response to MS1
    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.response}")
    private String routingKey;

    public void coordinate(CanonicalMessage message, String gatewayResponse) {

        String correlationId = message.getCorrelationId();

        log.info("📥 GATEWAY RESPONSE | {}", gatewayResponse);

        // Mock fraud processing
        String fraudResult = "APPROVED"; 
        log.info("🔍 FRAUD (MOCK) RESULT | {}", fraudResult);

        String finalDecision = combine(gatewayResponse, fraudResult);
        log.info("✅ FINAL RESPONSE | {}", finalDecision);

        // Build mock response
        TransactionResponse response = TransactionResponse.builder()
                .correlationId(correlationId)
                .status("SUCCESS")
                .responseCode("00")
                .responseMessage(finalDecision)
                .transactionId("TXN" + System.currentTimeMillis())
                .rrn("RRN" + System.currentTimeMillis())
                .approvalCode("APPR123")
                .balance("10000.00")
                .build();

        // Send response to MS1 via exchange + routing key
        log.info("📤 Sending response to exchange={} routingKey={} correlationId={}",
                exchange, routingKey, correlationId);
        rabbitTemplate.convertAndSend(exchange, routingKey, response);
        log.info("📤 SENT DONE | {}", correlationId);
    }

    private String combine(String gateway, String fraud) {
        if ("DECLINED".equalsIgnoreCase(fraud)) {
            return "DECLINED_BY_FRAUD";
        }
        return gateway;
    }
}