package com.paymenthub.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Base encryption utilities.
 * Framework-neutral, reusable across any Java application.
 */
@Slf4j
public class BaseEncryptionUtil {

    protected static final int AES_KEY_SIZE = 256;
    protected static final int GCM_IV_LENGTH = 16;
    protected static final int GCM_TAG_LENGTH = 128;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate 256-bit AES key
     */
    protected static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
        keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
        return keyGenerator.generateKey();
    }

    /**
     * Generate random IV
     */
    protected static byte[] generateIV(int length) {
        byte[] iv = new byte[length];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Encrypt data with AES-GCM
     */
    protected static byte[] encryptAESGCM(
            byte[] data, 
            byte[] keyBytes, 
            byte[] iv) throws Exception {
        
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, 
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(data);
    }

    /**
     * Encrypt data with AES-CBC
     */
    protected static byte[] encryptAESCBC(
            byte[] data, 
            byte[] keyBytes, 
            byte[] iv) throws Exception {
        
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    /**
     * Encrypt data with RSA
     */
    protected static byte[] encryptRSA(
            byte[] data, 
            PublicKey publicKey,
            String algorithm) throws Exception {
        
        Cipher cipher = Cipher.getInstance(algorithm, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Parse Base64 RSA public key
     */
    protected static PublicKey parsePublicKey(String base64PublicKey) 
            throws Exception {
        
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Base64 encode
     */
    protected static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 decode
     */
    protected static byte[] fromBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}