package com.paymenthub.ms1.controller;

import com.paymenthub.common.dto.TransactionResponse;
import com.paymenthub.ms1.service.SessionService;
import com.paymenthub.ms1.service.TransactionService;
import com.paymenthub.ms1.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transaction")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private SessionService sessionService;

    @Value("${encryption.client-aes-key}")
    private String clientAesKey;

    @PostMapping
    public ResponseEntity<Map<String, String>> processTransaction(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Source", defaultValue = "UNKNOWN") String source,
            @RequestHeader(value = "X-Destination", defaultValue = "NPCI") String destination,
            @RequestBody Map<String, String> request) {

        long start = System.currentTimeMillis();

        try {
            // Step 1: Validate session
            sessionService.validateSession(authorization);
            log.info("Request | source={} destination={}", source, destination);

            String encryptedPayload = request.get("encryptedPayload");

            // Step 2: Process — now passes source + destination
            TransactionResponse response =
                    transactionService.processTransaction(
                            encryptedPayload, source, destination);

            String responseJson = new com.fasterxml.jackson.databind
                    .ObjectMapper().writeValueAsString(response);
            String encryptedResponse = AESUtil.encrypt(responseJson, clientAesKey);

            log.info("✅ Total time: {}ms | {} | {}",
                    System.currentTimeMillis() - start,
                    response.getCorrelationId(),
                    response.getStatus());

            Map<String, String> result = new HashMap<>();
            result.put("encryptedResponse", encryptedResponse);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            if (e.getMessage() != null &&
               (e.getMessage().contains("session") ||
                e.getMessage().contains("Authorization"))) {
                Map<String, String> error = new HashMap<>();
                error.put("status", "UNAUTHORIZED");
                error.put("message", e.getMessage());
                return ResponseEntity.status(401).body(error);
            }
            log.error("❌ Failed after {}ms", System.currentTimeMillis() - start, e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "System error");
            return ResponseEntity.internalServerError().body(error);

        } catch (Exception e) {
            log.error("❌ Failed after {}ms", System.currentTimeMillis() - start, e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "System error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/test-encrypt")
    public ResponseEntity<Map<String, String>> testEncrypt(
            @RequestBody Object plainRequest) {
        try {
            String plainJson = new com.fasterxml.jackson.databind
                    .ObjectMapper().writeValueAsString(plainRequest);
            String encrypted = AESUtil.encrypt(plainJson, clientAesKey);
            Map<String, String> response = new HashMap<>();
            response.put("plainJson", plainJson);
            response.put("encryptedPayload", encrypted);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/test-decrypt")
    public ResponseEntity<Map<String, String>> testDecrypt(
            @RequestBody Map<String, String> request) {
        try {
            String encrypted = request.get("encryptedResponse");
            String decrypted = AESUtil.decrypt(encrypted, clientAesKey);
            Map<String, String> response = new HashMap<>();
            response.put("decryptedResponse", decrypted);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}