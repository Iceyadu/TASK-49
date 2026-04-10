package com.scholarops.service;

import com.scholarops.util.AesEncryptionUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    private final String aesEncryptionKey;

    public EncryptionService(@Qualifier("aesEncryptionKey") String aesEncryptionKey) {
        this.aesEncryptionKey = aesEncryptionKey;
    }

    /**
     * Encrypts plaintext using AES-256-CBC with the configured key.
     *
     * @param plaintext the text to encrypt
     * @return the encrypted data containing ciphertext and IV
     */
    public AesEncryptionUtil.EncryptedData encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext must not be null or empty");
        }
        return AesEncryptionUtil.encrypt(plaintext, aesEncryptionKey);
    }

    /**
     * Decrypts ciphertext using AES-256-CBC with the configured key.
     *
     * @param ciphertext the encrypted bytes
     * @param iv         the initialization vector used during encryption
     * @return the decrypted plaintext string
     */
    public String decrypt(byte[] ciphertext, byte[] iv) {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Ciphertext must not be null or empty");
        }
        if (iv == null || iv.length == 0) {
            throw new IllegalArgumentException("IV must not be null or empty");
        }
        return AesEncryptionUtil.decrypt(ciphertext, iv, aesEncryptionKey);
    }
}
