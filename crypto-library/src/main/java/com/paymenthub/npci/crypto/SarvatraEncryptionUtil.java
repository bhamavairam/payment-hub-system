package com.paymenthub.npci.crypto;

import com.paymenthub.crypto.BaseEncryptionUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * NPCI-specific encryption (Sarvatra format).
 * Implements NPCI's 5-parameter encryption: ct, sk, iv, api, ts
 * 
 * NPCI Spec:
 * - RSA: RSA/ECB/OAEPWithSHA1AndMGF1Padding
 * - AES: GCM or CBC mode
 * - IV: 16 bytes
 * - Timestamp format: yyyyMMddHHmmss
 */
@Slf4j
public class SarvatraEncryptionUtil extends BaseEncryptionUtil {

    private static final String RSA_ALGORITHM = 
            "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    
    private static final DateTimeFormatter TS_FORMAT = 
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Encrypt for NPCI (Sarvatra format)
     * 
     * @param plainJson JSON string to encrypt
     * @param apiCode NPCI API code
     * @param publicKeyBase64 NPCI public key
     * @param useGCM true for GCM, false for CBC
     * @return Map with ct, sk, iv, api, ts
     */
    public static Map<String, String> encrypt(
            String plainJson, 
            String apiCode, 
            String publicKeyBase64,
            boolean useGCM) throws Exception {
        
        log.debug("🔐 NPCI encryption | Mode: {}", useGCM ? "GCM" : "CBC");

        // Generate ephemeral AES key
        byte[] aesKeyBytes = generateAESKey().getEncoded();

        // Generate IV (16 bytes for NPCI)
        byte[] iv = generateIV(GCM_IV_LENGTH);

        // Parse public key
        PublicKey publicKey = parsePublicKey(publicKeyBase64);

        // Encrypt JSON with AES
        byte[] encryptedJson = useGCM 
                ? encryptAESGCM(plainJson.getBytes(StandardCharsets.UTF_8), aesKeyBytes, iv)
                : encryptAESCBC(plainJson.getBytes(StandardCharsets.UTF_8), aesKeyBytes, iv);
        String ct = toBase64(encryptedJson);

        // Encrypt AES key with RSA
        byte[] encryptedKey = encryptRSA(aesKeyBytes, publicKey, RSA_ALGORITHM);
        String sk = toBase64(encryptedKey);

        // Encrypt IV with RSA
        byte[] encryptedIV = encryptRSA(iv, publicKey, RSA_ALGORITHM);
        String ivEncrypted = toBase64(encryptedIV);

        // Encrypt API code with AES
        byte[] encryptedApi = useGCM
                ? encryptAESGCM(apiCode.getBytes(StandardCharsets.UTF_8), aesKeyBytes, iv)
                : encryptAESCBC(apiCode.getBytes(StandardCharsets.UTF_8), aesKeyBytes, iv);
        String api = toBase64(encryptedApi);

        // Encrypt timestamp with RSA
        String timestamp = LocalDateTime.now().format(TS_FORMAT);
        byte[] encryptedTs = encryptRSA(
                timestamp.getBytes(StandardCharsets.UTF_8), 
                publicKey, 
                RSA_ALGORITHM);
        String ts = toBase64(encryptedTs);

        // Build result
        Map<String, String> result = new HashMap<>();
        result.put("ct", ct);
        result.put("sk", sk);
        result.put("iv", ivEncrypted);
        result.put("api", api);
        result.put("ts", ts);

        log.debug("✅ NPCI encryption complete");
        return result;
    }
}