package com.paymenthub.ms1.controller;

import com.paymenthub.common.dto.TransactionResponse;
import com.paymenthub.ms1.service.DecryptionService;
import com.paymenthub.ms1.service.InboundProcessingService;
import com.paymenthub.ms1.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inbound")
@Slf4j
@RequiredArgsConstructor
public class InboundController {

    private final InboundProcessingService processingService;
    private final DecryptionService decryptionService;
    private final SessionService sessionService;

    /**
     * Main endpoint - receives encrypted payload from client
     * WAITS for FINAL response from MS-OUT-2
     */
    @PostMapping("/transaction")
    public ResponseEntity<Map<String, Object>> processTransaction(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Source", defaultValue = "UNKNOWN") String source,
            @RequestHeader(value = "X-Destination", required = false) String destination,
            @RequestBody Map<String, String> request) {
        
        long startTime = System.nanoTime();

        log.info("═══════════════════════════════════════════════════════");
        log.info("📥 INCOMING TRANSACTION REQUEST");
        log.info("═══════════════════════════════════════════════════════");

        try {
            // STEP 1: VALIDATE SESSION AND GET CLIENT ID
            String clientId = sessionService.validateSession(authorization);
            log.info("🔓 Session validated | clientId={} | source={} | destination={}", 
                    clientId, source, destination);

            // STEP 2: VALIDATE REQUEST
            String encryptedPayload = request.get("encryptedPayload");

            if (encryptedPayload == null || encryptedPayload.isEmpty()) {
                return buildErrorResponse("Missing encryptedPayload", HttpStatus.BAD_REQUEST);
            }

            // STEP 3: PROCESS TRANSACTION (pass clientId for decryption)
            TransactionResponse response = processingService.processInbound(
                    encryptedPayload, source, destination, clientId);  // ← PASS clientId

            long totalTime = (System.nanoTime() - startTime) / 1_000_000;

            log.info("═══════════════════════════════════════════════════════");
            log.info("✅ FINAL RESPONSE READY - {}ms", totalTime);
            log.info("Client: {} | Correlation: {}", clientId, response.getCorrelationId());
            log.info("Status: {} | Code: {}", response.getStatus(), response.getResponseCode());
            log.info("═══════════════════════════════════════════════════════");

            // Build response
            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("correlationId", response.getCorrelationId());
            apiResponse.put("transactionId", response.getTransactionId());
            apiResponse.put("status", response.getStatus());
            apiResponse.put("responseCode", response.getResponseCode());
            apiResponse.put("responseMessage", response.getResponseMessage());
            apiResponse.put("rrn", response.getRrn());
            apiResponse.put("approvalCode", response.getApprovalCode());
            apiResponse.put("balance", response.getBalance());
            apiResponse.put("processingTime", totalTime + "ms");

            return ResponseEntity.ok(apiResponse);

        } catch (RuntimeException e) {
            if (e.getMessage() != null &&
               (e.getMessage().contains("session") ||
                e.getMessage().contains("Authorization"))) {
                
                long totalTime = (System.nanoTime() - startTime) / 1_000_000;
                log.error("❌ AUTHENTICATION FAILED - {}ms: {}", totalTime, e.getMessage());
                
                return buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED);
            }

            long totalTime = (System.nanoTime() - startTime) / 1_000_000;
            log.error("❌ TRANSACTION FAILED - {}ms", totalTime, e);
            
            return buildErrorResponse("Transaction failed: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            long totalTime = (System.nanoTime() - startTime) / 1_000_000;
            log.error("❌ SYSTEM ERROR - {}ms", totalTime, e);

            return buildErrorResponse("System error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/test-encrypt")
    public ResponseEntity<Map<String, String>> testEncrypt(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Object plainRequest) {
        try {
            // Validate session and get clientId
            String clientId = sessionService.validateSession(authorization);
            log.info("🔐 Test encrypt request | clientId={}", clientId);

            String plainJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(plainRequest);
            
            // Encrypt using client-specific key
            String encrypted = decryptionService.encrypt(plainJson, clientId);  // ← PASS clientId

            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("plainJson", plainJson);
            response.put("encryptedPayload", encrypted);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("session")) {
                Map<String, String> error = new HashMap<>();
                error.put("status", "UNAUTHORIZED");
                error.put("message", e.getMessage());
                return ResponseEntity.status(401).body(error);
            }
            throw e;
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {

        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "microservice-in-1");
        response.put("timestamp",
                java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            String message,
            HttpStatus status) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", "ERROR");
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(status).body(error);
    }
}