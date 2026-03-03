package com.paymenthub.ms1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.common.dto.TransactionResponse;
import com.paymenthub.ms1.repository.InboundTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResponseListenerService {

    private final InboundTransactionRepository repository;
    private final InboundProcessingService transactionService;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            queues = "${rabbitmq.queues.from-ms2}",
            concurrency = "10-50"
    )
    @Transactional
    public void onResponse(TransactionResponse response) {

        log.info("📥 FINAL RESPONSE RECEIVED | {}",
                response.getCorrelationId());

        try {

            JsonNode responseNode = objectMapper.valueToTree(response);

            int updated = repository.updateStatusAndResponse(
                    response.getCorrelationId(),
                    response.getStatus(),
                    responseNode
            );

            if (updated > 0) {
                log.info("💾 DB UPDATED | {}", response.getCorrelationId());
            }

            transactionService.completeTransaction(response);

        } catch (Exception e) {
            log.error("❌ ERROR PROCESSING RESPONSE", e);
        }
    }
}