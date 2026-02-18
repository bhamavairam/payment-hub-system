package com.paymenthub.ms1.service;

import com.paymenthub.common.dto.TransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResponseListenerService {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private com.paymenthub.ms1.repository.ClientTransactionRepository repository;

    // ─────────────────────────────────────────────────────────────
    // Listens for responses from MS2
    // This runs on a SEPARATE thread from the request thread
    // But completes the CompletableFuture which wakes up the request thread
    // ─────────────────────────────────────────────────────────────
    @RabbitListener(queues = "${rabbitmq.queues.from-ms2}",
                    concurrency = "10-50")  // 10-50 concurrent listeners
    public void onResponse(TransactionResponse response) {
        
        long receiveTime = System.currentTimeMillis();
        
        log.info("📥 Response received from MS2: {} | Status: {} | Code: {}",
                response.getCorrelationId(),
                response.getStatus(),
                response.getResponseCode());

        // ── STEP 1: Update Database ───────────────────────────
        updateDatabase(response);

        // ── STEP 2: Wake up waiting request thread ────────────
        transactionService.completeTransaction(response);

        log.debug("⚡ Response processed in {}ms",
                System.currentTimeMillis() - receiveTime);
    }

    private void updateDatabase(TransactionResponse response) {
        try {
            String responsePayload = buildResponsePayload(response);
            
            int updated = repository.updateStatusAndResponse(
                    response.getCorrelationId(),
                    response.getStatus(),
                    responsePayload);

            if (updated > 0) {
                log.debug("💾 DB updated: {} → {}", 
                        response.getCorrelationId(), response.getStatus());
            } else {
                log.warn("⚠️ DB update failed for: {}", 
                        response.getCorrelationId());
            }
        } catch (Exception e) {
            log.error("❌ DB update error: {}", 
                    response.getCorrelationId(), e);
        }
    }

    private String buildResponsePayload(TransactionResponse response) {
        return String.format(
            "Code:%s|Msg:%s|TxnId:%s|RRN:%s|Approval:%s|Balance:%s",
            response.getResponseCode(),
            response.getResponseMessage(),
            response.getTransactionId(),
            response.getRrn(),
            response.getApprovalCode(),
            response.getBalance()
        );
    }
}