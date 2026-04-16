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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Real API boundary tests for GET /api/users/{id}, PUT /api/users/{id},
 * and POST /api/users/{id}/reset-password. No mocks — uses a real Spring
 * context and live MySQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserApiIntegrationTest {

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

        User targetUser = userRepository.findByUsername("user.api.target")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("user.api.target")
                        .email("user.api.target@scholarops.local")
                        .fullName("API Test Target User")
                        .passwordHash(passwordEncoder.encode("Target@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));
        targetUserId = targetUser.getId();

        // Ensure known password for each test run
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?",
                passwordEncoder.encode("Target@12345"), targetUserId);

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));
        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, targetUser.getId(), studentRole.getId());
        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    targetUser.getId(), studentRole.getId());
        }
    }

    // ---------- GET /api/users/{id} ----------

    @Test
    void adminCanGetUserById() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        mockMvc.perform(get("/api/users/" + targetUserId)
                        .header("Authorization", "Bearer " + adminTokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(targetUserId))
                .andExpect(jsonPath("$.data.username").value("user.api.target"));
    }

    @Test
    void unauthenticatedCannotGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/" + targetUserId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotGetUserById() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("user.api.target", "Target@12345");

        mockMvc.perform(get("/api/users/" + targetUserId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getForNonExistentUserReturns404() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        mockMvc.perform(get("/api/users/999999999")
                        .header("Authorization", "Bearer " + adminTokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- PUT /api/users/{id} ----------

    @Test
    void adminCanUpdateUser() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        Map<String, Object> body = Map.of(
                "email", "user.api.target.upd@scholarops.local",
                "fullName", "Updated Target Name");

        mockMvc.perform(put("/api/users/" + targetUserId)
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Target Name"));
    }

    @Test
    void studentCannotUpdateAnotherUser() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("user.api.target", "Target@12345");

        Map<String, Object> body = Map.of(
                "email", "hacked@scholarops.local",
                "fullName", "Hacked Name");

        mockMvc.perform(put("/api/users/" + targetUserId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateForNonExistentUserReturns404() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        Map<String, Object> body = Map.of(
                "email", "ghost@scholarops.local",
                "fullName", "Ghost User");

        mockMvc.perform(put("/api/users/999999999")
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- POST /api/users/{id}/reset-password ----------

    @Test
    void userCanResetOwnPassword() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("user.api.target", "Target@12345");

        Map<String, Object> body = Map.of(
                "oldPassword", "Target@12345",
                "newPassword", "NewTarget@99887");

        mockMvc.perform(post("/api/users/" + targetUserId + "/reset-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resetPasswordWithWrongOldPasswordReturnsUnauthorized() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("user.api.target", "Target@12345");

        Map<String, Object> body = Map.of(
                "oldPassword", "WrongOldPass@1234",
                "newPassword", "NewTarget@99887");

        mockMvc.perform(post("/api/users/" + targetUserId + "/reset-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unauthenticatedCannotResetPassword() throws Exception {
        Map<String, Object> body = Map.of(
                "oldPassword", "Target@12345",
                "newPassword", "NewTarget@99887");

        mockMvc.perform(post("/api/users/" + targetUserId + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotResetAnotherUsersPassword() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        Map<String, Object> body = Map.of(
                "oldPassword", "Target@12345",
                "newPassword", "NewTarget@99887");

        mockMvc.perform(post("/api/users/" + targetUserId + "/reset-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
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
