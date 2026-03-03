package com.paymenthub.npci.service;

import com.paymenthub.common.dto.CanonicalMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NpciProcessingService {

    private final NpciGatewayClient gatewayClient;
    private final ResponseCoordinatorService coordinator;

    public void process(CanonicalMessage message) {

        long start = System.nanoTime();

        try {

            String iso = gatewayClient.transform(message);
            String encrypted = gatewayClient.encrypt(iso);
            String gatewayResponse = gatewayClient.callGateway(encrypted);

            coordinator.coordinate(message, gatewayResponse);

        } catch (Exception e) {
            log.error("❌ NPCI PROCESSING FAILED", e);
        }

        long time = (System.nanoTime() - start) / 1_000_000;
        log.info("🏦 NPCI PROCESSING COMPLETED - {}ms", time);
    }
}