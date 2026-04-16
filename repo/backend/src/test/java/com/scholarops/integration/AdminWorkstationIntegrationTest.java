package com.scholarops.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.entity.Role;
import com.scholarops.model.entity.User;
import com.scholarops.repository.RoleRepository;
import com.scholarops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Real API boundary tests for admin password reset with workstation tracking.
 * Verifies that the X-Workstation-Id header is captured at the HTTP boundary,
 * that missing workstation ID is rejected, and that non-admins are forbidden.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminWorkstationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String ADMIN_PASSWORD = "Admin@12345678";
    private Long targetUserId;

    @BeforeEach
    void ensureTestDataExists() {
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE username = 'admin'",
                passwordEncoder.encode(ADMIN_PASSWORD));

        // Ensure a target user exists for the admin to reset
        User targetUser = userRepository.findByUsername("user.pwreset.target")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("user.pwreset.target")
                        .email("user.pwreset.target@scholarops.local")
                        .fullName("Password Reset Target User")
                        .passwordHash(passwordEncoder.encode("OldPass@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));
        targetUserId = targetUser.getId();

        // Ensure this user has STUDENT role so they're a valid non-admin user too
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));
        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, targetUserId, studentRole.getId());
        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    targetUserId, studentRole.getId());
        }
    }

    @Test
    void adminCanResetPasswordWithWorkstationIdInBody() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        Map<String, Object> body = Map.of(
                "newPassword", "NewSecure@Pass9876",
                "workstationId", "WS-BODY-001",
                "reason", "Integration test password reset");

        mockMvc.perform(post("/api/users/" + targetUserId + "/admin-reset-password")
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void xWorkstationIdHeaderTakesPrecedenceOverBodyWorkstationId() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        // Send body with one workstation ID and header with a different one.
        // The header should take precedence and be stored in the audit log.
        Map<String, Object> body = Map.of(
                "newPassword", "HeaderTest@Pass4321",
                "workstationId", "WS-BODY-PLACEHOLDER",
                "reason", "Testing header override");

        mockMvc.perform(post("/api/users/" + targetUserId + "/admin-reset-password")
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .header("X-Workstation-Id", "WS-HEADER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify the audit log captured the header workstation ID, not the body value
        List<String> workstationAuditValues = jdbcTemplate.queryForList(
                "SELECT workstation_id FROM audit_logs WHERE action = 'USER_ADMIN_PASSWORD_RESET' " +
                "AND entity_id = ? ORDER BY created_at DESC LIMIT 1",
                String.class, targetUserId);
        assertNotNull(workstationAuditValues, "Audit log should exist");
        assertEquals("WS-HEADER-001", workstationAuditValues.get(0),
                "Audit log must record the X-Workstation-Id header value, not the body workstationId");
    }

    @Test
    void missingWorkstationIdInBodyReturnsBadRequest() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        // workstationId is @NotBlank in the DTO, so omitting it causes a 400
        Map<String, Object> body = Map.of(
                "newPassword", "MissingWs@Pass5678",
                "reason", "Test without workstation");

        mockMvc.perform(post("/api/users/" + targetUserId + "/admin-reset-password")
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void nonAdminCannotAdminResetPassword() throws Exception {
        // The target user has STUDENT role, not ADMINISTRATOR
        AuthTokens studentTokens = loginAndExtractTokens("student.integration", "Student@12345");

        Map<String, Object> body = Map.of(
                "newPassword", "Forbidden@Pass9012",
                "workstationId", "WS-STUDENT-001");

        mockMvc.perform(post("/api/users/" + targetUserId + "/admin-reset-password")
                        .header("Authorization", "Bearer " + studentTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unauthenticatedCannotAdminResetPassword() throws Exception {
        Map<String, Object> body = Map.of(
                "newPassword", "Unauthenticated@Pass1234",
                "workstationId", "WS-ANON-001");

        mockMvc.perform(post("/api/users/" + targetUserId + "/admin-reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminResetPasswordForNonExistentUserReturns404() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        Map<String, Object> body = Map.of(
                "newPassword", "NotFound@Pass9876",
                "workstationId", "WS-404-001");

        mockMvc.perform(post("/api/users/999999999/admin-reset-password")
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private AuthTokens loginAndExtractTokens(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(root.path("data").path("accessToken").asText(),
                root.path("data").path("refreshToken").asText());
    }

    private record AuthTokens(String accessToken, String refreshToken) {}
}
