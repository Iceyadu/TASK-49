package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.LoginRequest;
import com.scholarops.model.dto.LoginResponse;
import com.scholarops.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        LoginResponse refreshed = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "accessToken", refreshed.getAccessToken(),
                "refreshToken", refreshed.getRefreshToken())));
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
}
