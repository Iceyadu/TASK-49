package com.scholarops.service;

import com.scholarops.model.dto.LoginRequest;
import com.scholarops.model.dto.LoginResponse;
import com.scholarops.model.entity.Permission;
import com.scholarops.model.entity.Role;
import com.scholarops.model.entity.User;
import com.scholarops.model.entity.UserRole;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.UserRepository;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.security.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserDetailsServiceImpl userDetailsService,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getId());

            // Update last login time
            User user = userRepository.findById(userDetails.getId()).orElse(null);
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
            }

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                    .collect(Collectors.toList());

            List<String> permissions = userDetails.getPermissions() == null
                    ? List.of()
                    : List.copyOf(userDetails.getPermissions());

            try {
                auditLogService.log(
                        userDetails.getId(),
                        AuditAction.LOGIN_SUCCESS,
                        "User",
                        userDetails.getId(),
                        "User logged in successfully",
                        null,
                        null
                );
            } catch (Exception auditEx) {
                logger.warn("Failed to record login success audit for userId={}: {}", userDetails.getId(),
                        auditEx.getMessage());
            }

            logger.info("User '{}' logged in successfully", loginRequest.getUsername());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                    .user(LoginResponse.UserInfo.builder()
                            .username(userDetails.getUsername())
                            .email(userDetails.getEmail())
                            .build())
                    .roles(roles)
                    .permissions(permissions)
                    .build();

        } catch (AuthenticationException ex) {
            logger.warn("Login failed for username: {}", loginRequest.getUsername());

            userRepository.findByUsername(loginRequest.getUsername()).ifPresent(user -> {
                try {
                    auditLogService.log(
                            user.getId(),
                            AuditAction.LOGIN_FAILURE,
                            "User",
                            user.getId(),
                            "Login attempt failed: " + ex.getMessage(),
                            null,
                            null
                    );
                } catch (Exception auditEx) {
                    logger.warn("Failed to record login failure audit for userId={}: {}", user.getId(),
                            auditEx.getMessage());
                }
            });

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        // Atomically validate, extract userId, and blacklist the old refresh token
        // to prevent TOCTOU race conditions in concurrent refresh attempts
        Long userId = jwtTokenProvider.validateAndBlacklistRefreshToken(refreshToken);
        UserDetailsImpl userDetails;
        try {
            userDetails = (UserDetailsImpl) userDetailsService.loadUserById(userId);
        } catch (UsernameNotFoundException e) {
            throw new IllegalArgumentException("Invalid refresh token", e);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .collect(Collectors.toList());

        List<String> permissions = List.copyOf(userDetails.getPermissions());

        logger.debug("Access token refreshed for userId: {}", userId);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .username(userDetails.getUsername())
                        .email(userDetails.getEmail())
                        .build())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            jwtTokenProvider.blacklistToken(accessToken);
        }
        if (refreshToken != null) {
            jwtTokenProvider.blacklistToken(refreshToken);
        }
        logger.info("User tokens blacklisted on logout");
    }
}
