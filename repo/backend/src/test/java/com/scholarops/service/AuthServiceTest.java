package com.scholarops.service;

import com.scholarops.model.dto.LoginRequest;
import com.scholarops.model.dto.LoginResponse;
import com.scholarops.model.entity.User;
import com.scholarops.repository.UserRepository;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private UserDetailsImpl createUserDetails(Long id, String username) {
        User user = User.builder().id(id).username(username)
                .email(username + "@test.com").passwordHash("hash")
                .enabled(true).accountLocked(false).build();
        return new UserDetailsImpl(user, Set.of("ADMINISTRATOR"), Set.of("USER_MANAGE"));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("admin", "password");
        UserDetailsImpl userDetails = createUserDetails(1L, "admin");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.builder().id(1L).username("admin").build()));

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(auditLogService).log(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testRefreshToken() {
        UserDetailsImpl userDetails = createUserDetails(1L, "admin");

        when(jwtTokenProvider.validateAndBlacklistRefreshToken("valid-refresh")).thenReturn(1L);
        when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.refreshToken("valid-refresh");

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }
}
