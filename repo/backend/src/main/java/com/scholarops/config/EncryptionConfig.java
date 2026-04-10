package com.scholarops.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Value("${scholarops.aes.secret-key}")
    private String aesSecretKey;

    @Bean
    public String aesEncryptionKey() {
        if (aesSecretKey == null || aesSecretKey.length() != 32) {
            throw new IllegalStateException("AES-256 key must be exactly 32 characters (256 bits)");
        }
        return aesSecretKey;
    }
}
