package com.scholarops.service;

import com.scholarops.util.AesEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // 32-character key for AES-256
        encryptionService = new EncryptionService("01234567890123456789012345678901");
    }

    @Test
    void testEncryptDecrypt() {
        String plaintext = "Sensitive data to encrypt";

        AesEncryptionUtil.EncryptedData encrypted = encryptionService.encrypt(plaintext);
        assertNotNull(encrypted.getCiphertext());
        assertNotNull(encrypted.getIv());

        String decrypted = encryptionService.decrypt(
                encrypted.getCiphertext(), encrypted.getIv());
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testDifferentIvEachTime() {
        String plaintext = "Same plaintext";

        AesEncryptionUtil.EncryptedData first = encryptionService.encrypt(plaintext);
        AesEncryptionUtil.EncryptedData second = encryptionService.encrypt(plaintext);

        // IVs should be different due to SecureRandom
        assertNotEquals(
                java.util.Base64.getEncoder().encodeToString(first.getIv()),
                java.util.Base64.getEncoder().encodeToString(second.getIv())
        );

        // But both should decrypt to the same plaintext
        assertEquals(plaintext, encryptionService.decrypt(first.getCiphertext(), first.getIv()));
        assertEquals(plaintext, encryptionService.decrypt(second.getCiphertext(), second.getIv()));
    }
}
