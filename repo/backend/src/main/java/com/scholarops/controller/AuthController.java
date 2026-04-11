package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.LoginRequest;
import com.scholarops.model.dto.LoginResponse;
import com.scholarops.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AuthenticationException ex) {
            return unauthorizedLogin();
        } catch (Exception ex) {
            // Map any other failure (wrapped persistence, etc.) to 401 so clients never see 500 on login attempts.
            log.warn("Login failed: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
            return unauthorizedLogin();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            LoginResponse refreshed = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "accessToken", refreshed.getAccessToken(),
                    "refreshToken", refreshed.getRefreshToken())));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(msg != null ? msg : "Invalid refresh token"));
        } catch (Exception ex) {
            log.warn("Refresh failed: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> body) {
        String accessToken = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        String refreshToken = body != null ? body.get("refreshToken") : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private static ResponseEntity<ApiResponse<LoginResponse>> unauthorizedLogin() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password"));
    }
}
