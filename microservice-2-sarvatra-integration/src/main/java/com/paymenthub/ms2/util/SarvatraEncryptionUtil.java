package com.paymenthub.ms2.util;

import com.paymenthub.common.exception.EncryptionException;
import com.paymenthub.ms2.dto.SarvatraEncryptedRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class SarvatraEncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(SarvatraEncryptionUtil.class);
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static SarvatraEncryptedRequest encryptForSarvatra(String jsonPayload, String apiCode, String sarvatraPublicKeyBase64) {
        try {
            log.info("Starting Sarvatra encryption");

            // Generate ephemeral AES key
            SecretKey aesKey = generateAESKey();
            byte[] aesKeyBytes = aesKey.getEncoded();

            // Generate IV
            byte[] iv = generateIV();
            byte[] ivApi = generateIV();

            // Parse RSA public key
            PublicKey publicKey = parsePublicKey(sarvatraPublicKeyBase64);

            // Encrypt payload
            String ct = encryptWithAES(jsonPayload, aesKeyBytes, iv);
            String sk = encryptWithRSA(aesKeyBytes, publicKey);
            String ivEncrypted = encryptWithRSA(iv, publicKey);
            String apiEncrypted = encryptWithAES(apiCode, aesKeyBytes, ivApi);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String ts = encryptWithRSA(timestamp.getBytes(StandardCharsets.UTF_8), publicKey);

            log.info("Sarvatra encryption completed");

            return SarvatraEncryptedRequest.builder()
                    .ct(ct)
                    .sk(sk)
                    .iv(ivEncrypted)
                    .api(apiEncrypted)
                    .ts(ts)
                    .build();

        } catch (Exception e) {
        	log.error("Sarvatra encryption failed: {}", e.getMessage(), e);
            throw new EncryptionException("Failed to encrypt for Sarvatra", e);
        }
    }

    private static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, new SecureRandom());
        return keyGenerator.generateKey();
    }

    private static byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static String encryptWithAES(String data, byte[] keyBytes, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM, "BC");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static String encryptWithRSA(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static PublicKey parsePublicKey(String base64PublicKey) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}