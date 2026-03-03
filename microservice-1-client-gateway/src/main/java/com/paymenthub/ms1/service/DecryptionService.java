package com.paymenthub.ms1.service;

import com.paymenthub.ms1.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
@Slf4j
public class DecryptionService {

    @Value("${encryption.client-aes-key}")
    private String clientAesKey;

    private SecretKey secretKey;

    /**
     * Load and prepare AES key once at startup
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(clientAesKey);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("🔐 AES key initialized at startup");
    }

    /**
     * Decrypt encrypted payload from client
     */
    public String decrypt(String encryptedPayload) {
        long startTime = System.nanoTime();

        try {
            String decrypted = AESUtil.decrypt(encryptedPayload, secretKey);

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            log.info("⏱️ Actual AES crypto time: {} ms", duration);

            return decrypted;

        } catch (Exception e) {
            log.error("❌ Decryption failed", e);
            throw new RuntimeException("Failed to decrypt payload", e);
        }
    }

    /**
     * Encrypt response to send back to client
     */
    public String encrypt(String plainText) {
        try {
            return AESUtil.encrypt(plainText, secretKey);
        } catch (Exception e) {
            log.error("❌ Encryption failed", e);
            throw new RuntimeException("Failed to encrypt response", e);
        }
    }
}