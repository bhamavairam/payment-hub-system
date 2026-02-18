package com.paymenthub.ms2.controller;

import com.paymenthub.ms2.dto.SarvatraEncryptedRequest;
import com.paymenthub.ms2.dto.SarvatraResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction")
@Slf4j
public class MockSarvatraController {

    @PostMapping("/process")
    public SarvatraResponse mockSarvatraEndpoint(@RequestBody SarvatraEncryptedRequest request,
                                                   @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("Mock Sarvatra received request - Correlation ID: {}", correlationId);
        log.info("Encrypted data received: ct={}, sk={}, iv={}, api={}, ts={}", 
                request.getCt().substring(0, 20) + "...",
                request.getSk().substring(0, 20) + "...",
                request.getIv().substring(0, 20) + "...",
                request.getApi().substring(0, 20) + "...",
                request.getTs().substring(0, 20) + "...");

        // Simulate processing delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Return success response
        return SarvatraResponse.builder()
                .responseCode("00")
                .responseMessage("Transaction Successful")
                .transactionId("SARV-" + System.currentTimeMillis())
                .rrn("RRN" + System.currentTimeMillis())
                .approvalCode("APP" + (int)(Math.random() * 1000000))
                .balance("50000.00")
                .build();
    }
}