package com.paymenthub.ms2.service;

import com.paymenthub.ms2.dto.TransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResponsePublisherService {

    @Autowired
    private KafkaTemplate<String, TransactionResponse> kafkaTemplate;

    @Value("${kafka.topics.to-ms1}")
    private String toMs1Topic;

    public void sendResponseToMs1(TransactionResponse response) {
        try {
            log.info("Sending response to MS1 - Correlation ID: {}", response.getCorrelationId());
            kafkaTemplate.send(toMs1Topic, response.getCorrelationId(), response);
            log.info("Response sent to MS1 successfully - Correlation ID: {}", response.getCorrelationId());
        } catch (Exception e) {
            log.error("Failed to send response to MS1 - Correlation ID: {}", 
                    response.getCorrelationId(), e);
        }
    }
}