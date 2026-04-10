package com.scholarops.security;

import com.scholarops.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Generate a 256-bit (32-byte) key encoded in Base64
        byte[] keyBytes = new byte[32];
        java.util.Arrays.fill(keyBytes, (byte) 0x41);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        jwtTokenProvider = new JwtTokenProvider(base64Key, 3600000L, 86400000L);
    }

    private Authentication createAuthentication(Long userId, String username) {
        User user = User.builder().id(userId).username(username)
                .email(username + "@test.com").passwordHash("hash")
                .enabled(true).accountLocked(false).build();
        UserDetailsImpl userDetails = new UserDetailsImpl(user,
                Set.of("ADMINISTRATOR"), Set.of("USER_MANAGE"));
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void testGenerateAndValidateToken() {
        Authentication auth = createAuthentication(1L, "testuser");
        String token = jwtTokenProvider.generateAccessToken(auth);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testExpiredToken() {
        // Create a provider with 0ms expiration
        byte[] keyBytes = new byte[32];
        java.util.Arrays.fill(keyBytes, (byte) 0x41);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(base64Key, 0L, 0L);

        Authentication auth = createAuthentication(1L, "testuser");
        String token = expiredProvider.generateAccessToken(auth);

        assertFalse(expiredProvider.validateToken(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.token"));
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void testGetUserIdFromToken() {
        Authentication auth = createAuthentication(42L, "admin");
        String token = jwtTokenProvider.generateAccessToken(auth);

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals(42L, userId);
    }
}
