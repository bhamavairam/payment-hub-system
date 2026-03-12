package com.paymenthub.ms1.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AESUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypt with Base64 encoded key string
     */
    public static String encrypt(String plainText, String base64Key) throws Exception {
        SecretKey secretKey = base64ToSecretKey(base64Key);
        return encrypt(plainText, secretKey);
    }

    /**
     * Encrypt with SecretKey object
     */
    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey,
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] encrypted = cipher.doFinal(
                plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt with Base64 encoded key string
     */
    public static String decrypt(String encryptedData, String base64Key) throws Exception {
        SecretKey secretKey = base64ToSecretKey(base64Key);
        return decrypt(encryptedData, secretKey);
    }

    /**
     * Decrypt with SecretKey object
     */
    public static String decrypt(String encryptedData, SecretKey secretKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);
        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey,
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Convert Base64 encoded key string to SecretKey
     */
    private static SecretKey base64ToSecretKey(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, "AES");
    }
}