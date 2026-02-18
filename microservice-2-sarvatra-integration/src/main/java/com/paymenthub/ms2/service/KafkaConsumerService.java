package com.paymenthub.ms2.service;

import com.paymenthub.common.dto.InternalTransactionMessage;
import com.paymenthub.ms2.dto.SarvatraEncryptedRequest;
import com.paymenthub.ms2.dto.SarvatraResponse;
import com.paymenthub.ms2.dto.TransactionResponse;
import com.paymenthub.ms2.util.AESUtil;
import com.paymenthub.ms2.util.SarvatraEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private SarvatraCommunicationService sarvatraCommunicationService;

    @Autowired
    private ResponsePublisherService responsePublisherService;

    // DUMMY KEYS
    private static final String INTERNAL_AES_KEY = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=";
    private static final String SARVATRA_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk7VDePuPdR9FfYsmdBcVmUFEhnHqZ2GVnqdZRFKp34c65qmNjrCKAlEPjMd6pJjFq3ivjocXMeYVrw2MTCWlBBKqU+fCv96lISC6IAW9NGqErAbvz5C7BEW4X32JzO9SIe9YnzgTF1Bt5lxkQqLSBBGB31GuvWH+juKgDIzAhjW04fMrX0stH2DFncZe96Jd8H2eathskHLuyz7oyokxF2AU9OcgZNhs8InQ4pnsWBwY/kUqhGxWRK3z9fCR5dLjhHUYW0pk8IP3nU4kKwOiRzTlw3Ne3ROznUmJ25vYUZ8rijgp2l1QJWZOsTVMv4Bb8UB036d7bZtI1KBQmN4OuwIDAQAB";

    @KafkaListener(topics = "${kafka.topics.from-ms1}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeFromMs1(InternalTransactionMessage message) {
        String correlationId = message.getCorrelationId();
        
        try {
            log.info("Received from MS1: {}", correlationId);

            // Step 1: Decrypt
            String decryptedJson = AESUtil.decrypt(message.getEncryptedPayload(), INTERNAL_AES_KEY);
            log.info("Decrypted: {}", decryptedJson);

            // Step 2: Encrypt for Sarvatra
            SarvatraEncryptedRequest sarvatraRequest = SarvatraEncryptionUtil.encryptForSarvatra(
                    decryptedJson, "TRANSACTION_API", SARVATRA_PUBLIC_KEY);

            log.info("Encrypted for Sarvatra: {}", correlationId);
            log.info("Sarvatra request: ct={}, sk={}, iv={}, api={}, ts={}", 
                    sarvatraRequest.getCt().substring(0, Math.min(20, sarvatraRequest.getCt().length())) + "...",
                    sarvatraRequest.getSk().substring(0, Math.min(20, sarvatraRequest.getSk().length())) + "...",
                    sarvatraRequest.getIv().substring(0, Math.min(20, sarvatraRequest.getIv().length())) + "...",
                    sarvatraRequest.getApi().substring(0, Math.min(20, sarvatraRequest.getApi().length())) + "...",
                    sarvatraRequest.getTs().substring(0, Math.min(20, sarvatraRequest.getTs().length())) + "...");

            // Step 3: Send to Sarvatra Switch
            sarvatraCommunicationService.sendToSarvatraSync(correlationId, sarvatraRequest)
                    .subscribe(
                            sarvatraResponse -> handleSarvatraResponse(correlationId, sarvatraResponse),
                            error -> handleSarvatraError(correlationId, error)
                    );

        } catch (Exception e) {
            log.error("Error processing from MS1: {}", correlationId, e);
            sendErrorResponse(correlationId, "Processing failed: " + e.getMessage());
        }
    }

    private void handleSarvatraResponse(String correlationId, SarvatraResponse sarvatraResponse) {
        log.info("Sarvatra response received - Correlation ID: {}, Response Code: {}", 
                correlationId, sarvatraResponse.getResponseCode());

        // Build response to send back to MS1
        TransactionResponse response = TransactionResponse.builder()
                .correlationId(correlationId)
                .status(determineStatus(sarvatraResponse.getResponseCode()))
                .responseCode(sarvatraResponse.getResponseCode())
                .responseMessage(sarvatraResponse.getResponseMessage())
                .transactionId(sarvatraResponse.getTransactionId())
                .rrn(sarvatraResponse.getRrn())
                .approvalCode(sarvatraResponse.getApprovalCode())
                .timestamp(System.currentTimeMillis())
                .build();

        // Send response back to MS1
        responsePublisherService.sendResponseToMs1(response);
    }

    private void handleSarvatraError(String correlationId, Throwable error) {
        log.error("Sarvatra communication error - Correlation ID: {}", correlationId, error);
        sendErrorResponse(correlationId, "Sarvatra error: " + error.getMessage());
    }

    private void sendErrorResponse(String correlationId, String errorMessage) {
        TransactionResponse response = TransactionResponse.builder()
                .correlationId(correlationId)
                .status("FAILED")
                .responseCode("ERROR")
                .responseMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        responsePublisherService.sendResponseToMs1(response);
    }

    private String determineStatus(String responseCode) {
        if (responseCode == null) return "FAILED";
        
        // Adjust these codes based on actual Sarvatra response codes
        switch (responseCode) {
            case "00":
            case "SUCCESS":
                return "SUCCESS";
            case "TIMEOUT":
                return "TIMEOUT";
            default:
                return "FAILED";
        }
    }
}