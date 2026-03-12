package com.paymenthub.ms1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymenthub.common.dto.CanonicalMessage;
import com.paymenthub.common.dto.TransactionResponse;
import com.paymenthub.ms1.entity.InboundTransaction;
import com.paymenthub.ms1.repository.InboundTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboundProcessingService {

    private final DecryptionService decryptionService;
    private final CanonicalConverterService canonicalConverter;
    private final GatewayResolverService gatewayResolver;
    private final InboundTransactionRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.router}")
    private String routerRoutingKey;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ═══════════════════════════════════════════════════════
    // PENDING TRANSACTIONS MAP (CRITICAL!)
    // ═══════════════════════════════════════════════════════
    private final Map<String, CompletableFuture<TransactionResponse>> pendingMap =
            new ConcurrentHashMap<>();

    /**
     * Main processing pipeline
     */
    public TransactionResponse processInbound(String encryptedPayload,
                                              String source,
                                              String destination,String clientId) throws Exception {

        long totalStart = System.nanoTime();

        // STEP 1: Decrypt
        String plainPayload = decryptionService.decrypt(encryptedPayload, clientId); 

        // STEP 2: Convert to Canonical
        CanonicalMessage message = canonicalConverter.convert(plainPayload);

        // Add source metadata
        if (message.getAdditionalData() == null) {
            message.setAdditionalData(new java.util.HashMap<>());
        }
        message.getAdditionalData().put("source", source);

        // STEP 3: Resolve Gateway (with destination hint support)
        if (destination != null && !destination.isEmpty() && !"UNKNOWN".equals(destination)) {
            log.info("🎯 Using destination hint: {}", destination);
            message.setTargetGateway(destination);
        } else {
            log.info("🔍 Resolving gateway from card BIN");
            gatewayResolver.resolveDestinationGateway(message);
        }

        log.info("📥 Inbound | CorrId={} | TraceId={} | Dest={} | Amount={}",
                message.getCorrelationId(),
                message.getTraceId(),
                message.getTargetGateway(),
                message.getAmount());

        // ════════════════════════════════════════════════════
        // STEP 4: CREATE FUTURE BEFORE PUBLISHING (CRITICAL!)
        // ════════════════════════════════════════════════════
        CompletableFuture<TransactionResponse> future = new CompletableFuture<>();
        pendingMap.put(message.getCorrelationId(), future);

        try {
            // STEP 5: Parallel DB + MQ
            CompletableFuture<Void> dbSave = saveToDatabase(message, encryptedPayload);
            CompletableFuture<Void> mqPublish = publishToRouter(message);

            CompletableFuture.allOf(dbSave, mqPublish).get();

            long preResponseTime = (System.nanoTime() - totalStart) / 1_000_000;
            log.info("⏱️ Pre-response processing: {}ms", preResponseTime);

            // ════════════════════════════════════════════════════
            // STEP 6: WAIT FOR RESPONSE (max 10 seconds)
            // ════════════════════════════════════════════════════
            log.info("⏳ Waiting for gateway response...");
            
            TransactionResponse response = future.get(10, TimeUnit.SECONDS);

            long totalTime = (System.nanoTime() - totalStart) / 1_000_000;
            log.info("✅ TOTAL END-TO-END TIME: {}ms | CorrId={} | Status={}",
                    totalTime,
                    message.getCorrelationId(),
                    response.getStatus());

            return response;

        } catch (TimeoutException e) {
            // Remove from pending map on timeout
            pendingMap.remove(message.getCorrelationId());
            
            log.error("⏰ TIMEOUT waiting for response | CorrId={}", 
                    message.getCorrelationId());
            
            // Return timeout response
            return TransactionResponse.builder()
                    .correlationId(message.getCorrelationId())
                    .status("TIMEOUT")
                    .responseCode("68")
                    .responseMessage("Transaction timed out")
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            // Remove from pending map on error
            pendingMap.remove(message.getCorrelationId());
            throw e;
        }
    }

    /**
     * Save inbound to PostgreSQL (parallel)
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> saveToDatabase(CanonicalMessage message,
                                                  String encryptedPayload) {
        try {
            JsonNode canonicalNode = objectMapper.valueToTree(message);

            InboundTransaction transaction = InboundTransaction.builder()
                    .correlationId(message.getCorrelationId())
                    .traceId(message.getTraceId())
                    .clientFormat(message.getOriginalFormat())
                    .encryptedPayload(encryptedPayload)
                    .canonicalMessage(canonicalNode)
                    .targetGateway(message.getTargetGateway())
                    .cardNetwork(message.getCardNetwork())
                    .cardBIN(message.getCardBIN())
                    .status("RECEIVED")
                    .build();

            repository.save(transaction);
            log.debug("💾 DB Saved | {}", message.getCorrelationId());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ DB Save Failed | {}", message.getCorrelationId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Publish to Router queue
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> publishToRouter(CanonicalMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routerRoutingKey,
                    message,
                    msg -> {
                        msg.getMessageProperties()
                                .setCorrelationId(message.getCorrelationId());
                        return msg;
                    });

            log.debug("📤 MQ Published | {}", message.getCorrelationId());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ MQ Publish Failed | {}", message.getCorrelationId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Called by ResponseListenerService when response arrives
     */
    public void completeTransaction(TransactionResponse response) {
        
        String correlationId = response.getCorrelationId();
        CompletableFuture<TransactionResponse> future = pendingMap.remove(correlationId);

        if (future != null) {
            future.complete(response);
            log.info("✅ Future completed | {} | Status={}", 
                    correlationId, response.getStatus());
        } else {
            log.warn("⚠️ No pending future found | {} | This might be a duplicate or late response", 
                    correlationId);
        }
    }
}