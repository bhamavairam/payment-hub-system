package com.paymenthub.ms1.service;

import com.paymenthub.ms1.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DecryptionService {

    private final ClientKeyService clientKeyService;

    /**
     * Decrypt encrypted payload from client
     * 
     * @param encryptedPayload - Base64 encoded encrypted data
     * @param clientId - Client ID to get the correct AES key
     */
    public String decrypt(String encryptedPayload, String clientId) {
        
        long startTime = System.nanoTime();
        
        try {
            log.debug("🔓 Decrypting payload | clientId={}", clientId);
            
            // Get client-specific AES key (from cache or DB)
            String clientAesKey = clientKeyService.getClientAesKey(clientId);
            
            // Decrypt using client's AES key
            String decrypted = AESUtil.decrypt(encryptedPayload, clientAesKey);
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            log.info("⏱️ Actual AES crypto time: {} ms", duration);
            
            return decrypted;
            
        } catch (Exception e) {
            log.error("❌ Decryption failed | clientId={}", clientId, e);
            throw new RuntimeException("Failed to decrypt payload for client: " + clientId, e);
        }
    }

    /**
     * Encrypt response to send back to client
     */
    public String encrypt(String plainText, String clientId) {
        try {
            // Get client-specific AES key
            String clientAesKey = clientKeyService.getClientAesKey(clientId);
            
            // Encrypt using client's AES key
            return AESUtil.encrypt(plainText, clientAesKey);
            
        } catch (Exception e) {
            log.error("❌ Encryption failed | clientId={}", clientId, e);
            throw new RuntimeException("Failed to encrypt response for client: " + clientId, e);
        }
    }
}