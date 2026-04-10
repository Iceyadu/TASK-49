package com.scholarops.security;

import com.scholarops.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenBlacklistTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
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
    void blacklistedTokenFailsValidation() {
        Authentication auth = createAuthentication(1L, "testuser");
        String token = jwtTokenProvider.generateAccessToken(auth);

        assertTrue(jwtTokenProvider.validateToken(token));

        jwtTokenProvider.blacklistToken(token);

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void logoutBlacklistsBothAccessAndRefreshTokens() {
        Authentication auth = createAuthentication(1L, "testuser");
        String accessToken = jwtTokenProvider.generateAccessToken(auth);
        String refreshToken = jwtTokenProvider.generateRefreshToken(1L);

        assertTrue(jwtTokenProvider.validateToken(accessToken));
        assertTrue(jwtTokenProvider.validateRefreshToken(refreshToken));

        jwtTokenProvider.blacklistToken(accessToken);
        jwtTokenProvider.blacklistToken(refreshToken);

        assertFalse(jwtTokenProvider.validateToken(accessToken));
        assertFalse(jwtTokenProvider.validateRefreshToken(refreshToken));
    }

    @Test
    void refreshTokenRotationBlacklistsOldRefreshToken() throws InterruptedException {
        String oldRefreshToken = jwtTokenProvider.generateRefreshToken(1L);
        Thread.sleep(1100);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(1L);

        assertNotEquals(oldRefreshToken, newRefreshToken);
        assertTrue(jwtTokenProvider.validateRefreshToken(oldRefreshToken));
        assertTrue(jwtTokenProvider.validateRefreshToken(newRefreshToken));

        jwtTokenProvider.blacklistToken(oldRefreshToken);

        assertFalse(jwtTokenProvider.validateRefreshToken(oldRefreshToken));
        assertTrue(jwtTokenProvider.validateRefreshToken(newRefreshToken));
    }

    @Test
    void nonBlacklistedTokensStillValidate() {
        Authentication auth1 = createAuthentication(1L, "testuser");
        Authentication auth2 = createAuthentication(2L, "otheruser");
        String token1 = jwtTokenProvider.generateAccessToken(auth1);
        String token2 = jwtTokenProvider.generateAccessToken(auth2);

        assertNotEquals(token1, token2);
        jwtTokenProvider.blacklistToken(token1);

        assertFalse(jwtTokenProvider.validateToken(token1));
        assertTrue(jwtTokenProvider.validateToken(token2));
    }
}
