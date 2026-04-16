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

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RealApiBoundaryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    private static final String ADMIN_PASSWORD = "Admin@12345678";

    @BeforeEach
    void ensureStudentUserExistsWithRole() {
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE username = 'admin'",
                passwordEncoder.encode(ADMIN_PASSWORD));

        User student = userRepository.findByUsername("student.integration")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("student.integration")
                        .email("student.integration@scholarops.local")
                        .fullName("Student Integration")
                        .passwordHash(passwordEncoder.encode("Student@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, student.getId(), studentRole.getId());

        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    student.getId(), studentRole.getId());
        }
    }

    @Test
    void invalidLoginReturns401AndErrorPayload() throws Exception {
        Map<String, String> body = Map.of(
                "username", "admin",
                "password", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void adminBearerTokenCanListUsersAndResponseHidesPasswordHash() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminTokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
    }

    @Test
    void studentBearerTokenIsForbiddenFromAdminUserListing() throws Exception {
        AuthTokens studentTokens = loginAndExtractTokens("student.integration", "Student@12345");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + studentTokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void refreshTokenIsRejectedAfterLogoutBlacklisting() throws Exception {
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", adminTokens.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", adminTokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    private AuthTokens loginAndExtractTokens(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = root.path("data").path("accessToken").asText();
        String refreshToken = root.path("data").path("refreshToken").asText();
        return new AuthTokens(accessToken, refreshToken);
    }

    private record AuthTokens(String accessToken, String refreshToken) {}
}
