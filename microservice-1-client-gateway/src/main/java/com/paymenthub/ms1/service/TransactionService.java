package com.paymenthub.ms1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymenthub.common.dto.RabbitMessage;
import com.paymenthub.common.dto.TransactionRequest;
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

    // In-memory map: correlationId → waiting CompletableFuture
    // This is how MS1 knows which response belongs to which request
    private final Map<String, CompletableFuture<TransactionResponse>> 
            pendingRequests = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ─────────────────────────────────────────────────────────────
    // MAIN METHOD: Called by Controller for each terminal request
    // ─────────────────────────────────────────────────────────────
    public TransactionResponse processTransaction(String encryptedPayload) 
            throws Exception {
        
        long startTime = System.currentTimeMillis();

        // ── STEP 1: DECRYPT (10ms) ────────────────────────────
        log.info("🔓 Decrypting terminal request...");
        String plainJson = AESUtil.decrypt(encryptedPayload, clientAesKey);
        log.debug("Plain JSON: {}", plainJson);

        // ── STEP 2: PARSE JSON ────────────────────────────────
        TransactionRequest request = objectMapper.readValue(
                plainJson, TransactionRequest.class);

        // Generate correlationId if not provided
        if (request.getCorrelationId() == null || 
            request.getCorrelationId().isEmpty()) {
            request.setCorrelationId("TXN-" + UUID.randomUUID()
                    .toString().substring(0, 8).toUpperCase());
        }
        String correlationId = request.getCorrelationId();
        
        log.info("📥 Transaction: {} | Terminal: {} | Type: {} | Amount: {}", 
                correlationId, request.getTerminalId(), 
                request.getTxnType(), request.getAmount());

        // ── STEP 3: REGISTER FUTURE (before parallel ops) ────
        // Register FIRST so response is never missed!
        CompletableFuture<TransactionResponse> responseFuture = 
                new CompletableFuture<>();
        pendingRequests.put(correlationId, responseFuture);

        // ── STEP 4: PARALLEL - DB SAVE + RABBITMQ SEND ───────
        // Both operations run simultaneously!
        log.info("⚡ Starting parallel DB save + RabbitMQ send...");

        CompletableFuture<Void> dbSave = saveToDatabase(request, 
                                                         encryptedPayload);
        CompletableFuture<Void> mqSend = sendToRabbitMQ(correlationId, 
                                                          plainJson);

        // Wait for BOTH to complete
        CompletableFuture.allOf(dbSave, mqSend).get(5, TimeUnit.SECONDS);

        log.info("✅ DB saved + MQ sent in {}ms", 
                System.currentTimeMillis() - startTime);

        // ── STEP 5: WAIT FOR SARVATRA RESPONSE ───────────────
        log.info("⏳ Waiting for Sarvatra response... [{}]", correlationId);
        
        try {
            TransactionResponse response = responseFuture.get(
                    timeoutMs, TimeUnit.MILLISECONDS);
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ Response received in {}ms | Status: {}", 
                    totalTime, response.getStatus());

            return response;

        } catch (java.util.concurrent.TimeoutException e) {
            // Clean up if timeout
            pendingRequests.remove(correlationId);
            
            log.error("⏱️ TIMEOUT after {}ms for {}", 
                    System.currentTimeMillis() - startTime, correlationId);
            
            // Update DB to TIMEOUT
            repository.updateStatusAndResponse(correlationId, "TIMEOUT", 
                    "Transaction timed out");
            
            return TransactionResponse.builder()
                    .correlationId(correlationId)
                    .status("TIMEOUT")
                    .responseCode("91")
                    .responseMessage("Transaction timeout - Please try again")
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ASYNC: Save to database (runs in parallel)
    // ─────────────────────────────────────────────────────────────
    @Async("taskExecutor")
    public CompletableFuture<Void> saveToDatabase(TransactionRequest request, 
                                                   String encryptedPayload) {
        try {
            ClientTransaction transaction = ClientTransaction.builder()
                    .correlationId(request.getCorrelationId())
                    .terminalId(request.getTerminalId())
                    .txnType(request.getTxnType())
                    .amount(request.getAmount())
                    .cardNumber(maskCard(request.getCardNumber()))
                    .status("PENDING")
                    .requestPayload(encryptedPayload)
                    .build();

            repository.save(transaction);
            log.debug("💾 DB saved: {}", request.getCorrelationId());
            
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ DB save failed: {}", 
                    request.getCorrelationId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ASYNC: Send to RabbitMQ (runs in parallel with DB save)
    // ─────────────────────────────────────────────────────────────
    @Async("taskExecutor")
    public CompletableFuture<Void> sendToRabbitMQ(String correlationId, 
                                                    String plainJson) {
        try {
            RabbitMessage message = RabbitMessage.builder()
                    .correlationId(correlationId)
                    .plainJsonPayload(plainJson)
                    .timestamp(System.currentTimeMillis())
                    .build();

            rabbitTemplate.convertAndSend(exchange, toMs2RoutingKey, message);
            log.debug("📤 MQ sent: {}", correlationId);
            
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ MQ send failed: {}", correlationId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Called by ResponseListenerService when response arrives
    // ─────────────────────────────────────────────────────────────
    public void completeTransaction(TransactionResponse response) {
        String correlationId = response.getCorrelationId();
        
        CompletableFuture<TransactionResponse> future = 
                pendingRequests.remove(correlationId);

        if (future != null) {
            future.complete(response);
            log.info("🎯 Completed waiting request: {}", correlationId);
        } else {
            log.warn("⚠️ No waiting request found for: {}", correlationId);
        }
    }

    // Mask card number for security
    private String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) return cardNumber;
        return cardNumber.substring(0, 4) + "****" + 
               cardNumber.substring(cardNumber.length() - 4);
    }
}