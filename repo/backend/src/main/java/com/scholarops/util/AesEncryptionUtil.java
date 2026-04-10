package com.scholarops.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public final class AesEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private AesEncryptionUtil() {
        // Utility class
    }

    public static EncryptedData encrypt(String plaintext, String key) {
        try {
            SecretKeySpec secretKey = buildKey(key);
            byte[] iv = generateIv();
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptedData(ciphertext, iv);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public static String decrypt(byte[] ciphertext, byte[] iv, String key) {
        try {
            SecretKeySpec secretKey = buildKey(key);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] plainBytes = cipher.doFinal(ciphertext);

            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static SecretKeySpec buildKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static class EncryptedData {
        private final byte[] ciphertext;
        private final byte[] iv;

        public EncryptedData(byte[] ciphertext, byte[] iv) {
            this.ciphertext = ciphertext;
            this.iv = iv;
        }

        public byte[] getCiphertext() {
            return ciphertext;
        }

        public byte[] getIv() {
            return iv;
        }
    }
}
