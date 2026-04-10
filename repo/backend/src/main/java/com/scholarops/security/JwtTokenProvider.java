package com.scholarops.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long jwtExpirationMs;
    private final long refreshExpirationMs;
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public JwtTokenProvider(
            @Value("${scholarops.jwt.secret}") String jwtSecret,
            @Value("${scholarops.jwt.expiration-ms}") long jwtExpirationMs,
            @Value("${scholarops.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userDetails.getId().toString())
                .claim("username", userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("username", String.class);
    }

    public boolean validateToken(String token) {
        try {
            if (blacklistedTokens.contains(token)) {
                logger.warn("Token has been blacklisted");
                return false;
            }
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            // Reject refresh tokens used in access-token position
            if ("refresh".equals(claims.get("type", String.class))) {
                logger.warn("Refresh token used as access token");
                return false;
            }
            return true;
        } catch (MalformedJwtException e) {
            logger.warn("Invalid JWT token");
        } catch (ExpiredJwtException e) {
            logger.warn("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Validates a refresh token specifically. Unlike validateToken(), this method
     * requires the "type":"refresh" claim and rejects access tokens.
     */
    public boolean validateRefreshToken(String token) {
        try {
            if (blacklistedTokens.contains(token)) {
                logger.warn("Refresh token has been blacklisted");
                return false;
            }
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!"refresh".equals(claims.get("type", String.class))) {
                logger.warn("Non-refresh token used in refresh flow");
                return false;
            }
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            logger.warn("Invalid refresh token: {}", e.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * Atomically validates and blacklists a refresh token.
     * Returns the userId if valid, or throws IllegalArgumentException if invalid.
     * This prevents the TOCTOU race in refresh token rotation.
     */
    public Long validateAndBlacklistRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be null or blank");
        }
        synchronized (blacklistedTokens) {
            if (blacklistedTokens.contains(token)) {
                throw new IllegalArgumentException("Refresh token has already been used");
            }
            try {
                Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
                if (!"refresh".equals(claims.get("type", String.class))) {
                    throw new IllegalArgumentException("Token is not a refresh token");
                }
                blacklistedTokens.add(token);
                return Long.parseLong(claims.getSubject());
            } catch (JwtException e) {
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }
        }
    }

    public void blacklistToken(String token) {
        if (token != null && !token.isBlank()) {
            blacklistedTokens.add(token);
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void evictExpiredTokens() {
        blacklistedTokens.removeIf(token -> {
            try {
                Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
                return false; // still valid, keep it
            } catch (ExpiredJwtException e) {
                return true; // expired, safe to evict
            } catch (JwtException e) {
                return true; // invalid, evict
            }
        });
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
