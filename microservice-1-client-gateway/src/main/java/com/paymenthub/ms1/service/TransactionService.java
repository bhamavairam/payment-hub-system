package com.paymenthub.ms1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.common.dto.RabbitMessage;
import com.paymenthub.common.dto.TransactionResponse;
import com.paymenthub.ms1.entity.ClientTransaction;
import com.paymenthub.ms1.repository.ClientTransactionRepository;
import com.paymenthub.ms1.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TransactionService {

    @Autowired
    private ClientTransactionRepository repository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.to-ms2}")
    private String toMs2RoutingKey;

    @Value("${encryption.client-aes-key}")
    private String clientAesKey;

    @Value("${transaction.timeout-ms:28000}")
    private long timeoutMs;

    // correlationId → waiting thread
    private final Map<String, CompletableFuture<TransactionResponse>>
            pendingRequests = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    // CRITICAL PATH — everything here must be fast
    // Target: decrypt + send to MQ in under 20ms
    // ─────────────────────────────────────────────────────────────
    public TransactionResponse processTransaction(
            String encryptedPayload,
            String source,
            String destination) throws Exception {

        long startTime = System.currentTimeMillis();

        // ── Step 1: Decrypt (~5ms) ────────────────────────────────
        String plainJson = AESUtil.decrypt(encryptedPayload, clientAesKey);
        log.info("🔓 Decrypted in {}ms | src={} dest={}",
                System.currentTimeMillis() - startTime, source, destination);

        
        log.info("Decrypted Json", plainJson);
        
        // ── Step 2: Read field "11" (STAN) for correlationId ─────
        // Only reading one field — not validating anything
        @SuppressWarnings("unchecked")
        Map<String, String> isoFields = objectMapper.readValue(
                plainJson, Map.class);

        String stan = isoFields.get("11");
        String correlationId = (stan != null && !stan.isBlank())
                ? "TXN-" + stan
                : "TXN-" + UUID.randomUUID().toString()
                        .substring(0, 8).toUpperCase();

        log.info("📥 {} | MTI={} | Terminal={} | Dest={}",
                correlationId,
                isoFields.get("0"),
                isoFields.get("41"),
                destination);

        // ── Step 3: Register future BEFORE sending ────────────────
        CompletableFuture<TransactionResponse> responseFuture =
                new CompletableFuture<>();
        pendingRequests.put(correlationId, responseFuture);

        // ── Step 4: Send to RabbitMQ immediately (~10ms) ──────────
        // DB save runs fully in background — does NOT block MQ send
        RabbitMessage message = RabbitMessage.builder()
                .correlationId(correlationId)
                .plainJsonPayload(plainJson)
                .source(source)
                .destination(destination)
                .timestamp(System.currentTimeMillis())
                .build();

        rabbitTemplate.convertAndSend(exchange, toMs2RoutingKey, message);
        log.info("📤 Sent to MQ in {}ms | {}",
                System.currentTimeMillis() - startTime, correlationId);

        // ── Step 5: DB save fires in background ───────────────────
        // Client does NOT wait for this
        saveToDatabase(correlationId, isoFields, encryptedPayload);

        // ── Step 6: Wait for MS2/MS3 response ─────────────────────
        log.info("⏳ Waiting for response [{}]", correlationId);
        try {
            TransactionResponse response = responseFuture.get(
                    timeoutMs, TimeUnit.MILLISECONDS);

            log.info("✅ TOTAL {}ms | {} | {}",
                    System.currentTimeMillis() - startTime,
                    correlationId,
                    response.getStatus());
            return response;

        } catch (java.util.concurrent.TimeoutException e) {
            pendingRequests.remove(correlationId);
            log.error("⏱ TIMEOUT {}ms | {}",
                    System.currentTimeMillis() - startTime, correlationId);
            repository.updateStatusAndResponse(
                    correlationId, "TIMEOUT", "Transaction timed out");
            return TransactionResponse.builder()
                    .correlationId(correlationId)
                    .status("TIMEOUT")
                    .responseCode("91")
                    .responseMessage("Timeout - Please try again")
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    // ── Fully background — client never waits for this ────────────
    @Async("taskExecutor")
    public void saveToDatabase(
            String correlationId,
            Map<String, String> isoFields,
            String encryptedPayload) {
        try {
            ClientTransaction txn = ClientTransaction.builder()
                    .correlationId(correlationId)
                    .terminalId(isoFields.get("41"))
                    .txnType(isoFields.get("36"))
                    .status("PENDING")
                    .requestPayload(encryptedPayload)
                    .build();
            repository.save(txn);
            log.debug("💾 DB saved: {}", correlationId);
        } catch (Exception e) {
            log.error("❌ DB save failed: {}", correlationId, e);
        }
    }

    // ── Called by ResponseListenerService ─────────────────────────
    public void completeTransaction(TransactionResponse response) {
        CompletableFuture<TransactionResponse> future =
                pendingRequests.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response);
            log.info("🎯 Completed: {}", response.getCorrelationId());
        } else {
            log.warn("⚠️ No waiting request for: {}",
                    response.getCorrelationId());
        }
    }
}