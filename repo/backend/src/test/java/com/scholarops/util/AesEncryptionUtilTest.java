package com.scholarops.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionUtilTest {

    private static final String KEY_32 = "01234567890123456789012345678901";

    @Test
    void testEncryptDecryptRoundTrip() {
        String plaintext = "Hello, this is a secret message!";

        AesEncryptionUtil.EncryptedData encrypted = AesEncryptionUtil.encrypt(plaintext, KEY_32);
        assertNotNull(encrypted.getCiphertext());
        assertNotNull(encrypted.getIv());
        assertEquals(16, encrypted.getIv().length);

        String decrypted = AesEncryptionUtil.decrypt(
                encrypted.getCiphertext(), encrypted.getIv(), KEY_32);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testDifferentKeyFails() {
        String plaintext = "Secret data";
        String otherKey = "98765432109876543210987654321098";

        AesEncryptionUtil.EncryptedData encrypted = AesEncryptionUtil.encrypt(plaintext, KEY_32);

        assertThrows(IllegalStateException.class, () ->
                AesEncryptionUtil.decrypt(encrypted.getCiphertext(), encrypted.getIv(), otherKey));
    }

    @Test
    void testNullInput() {
        assertThrows(Exception.class, () ->
                AesEncryptionUtil.encrypt(null, KEY_32));
    }
}
