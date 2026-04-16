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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Real API boundary tests for GET /api/crawl-sources/{id} and
 * PUT /api/crawl-sources/{id}. Verifies ownership enforcement at the HTTP boundary.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrawlSourceApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long curatorUserId;
    private Long sourceId;

    @BeforeEach
    void ensureTestDataExists() {
        // Reuse curator.integration user created by CrawlWorkflowIntegrationTest
        User curator = userRepository.findByUsername("curator.integration")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("curator.integration")
                        .email("curator.integration@scholarops.local")
                        .fullName("Curator Integration User")
                        .passwordHash(passwordEncoder.encode("Curator@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));
        curatorUserId = curator.getId();

        Role curatorRole = roleRepository.findByName("CONTENT_CURATOR")
                .orElseThrow(() -> new IllegalStateException("CONTENT_CURATOR role not found"));
        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, curatorUserId, curatorRole.getId());
        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    curatorUserId, curatorRole.getId());
        }

        // Ensure a crawl source owned by this curator
        List<Long> sourceIds = jdbcTemplate.queryForList(
                "SELECT id FROM crawl_source_profiles WHERE name = 'source-api-test' AND created_by = ? LIMIT 1",
                Long.class, curatorUserId);
        if (sourceIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO crawl_source_profiles " +
                    "(name, base_url, description, rate_limit_per_minute, requires_auth, enabled, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    "source-api-test",
                    "https://example.org/source-api-test",
                    "Test source for GET/PUT API integration tests",
                    30, false, true, curatorUserId);
            sourceId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            sourceId = sourceIds.get(0);
        }
    }

    // ---------- GET /api/crawl-sources/{id} ----------

    @Test
    void curatorCanGetSourceById() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(get("/api/crawl-sources/" + sourceId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sourceId))
                .andExpect(jsonPath("$.data.name").value("source-api-test"));
    }

    @Test
    void unauthenticatedCannotGetSource() throws Exception {
        mockMvc.perform(get("/api/crawl-sources/" + sourceId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotGetSource() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        mockMvc.perform(get("/api/crawl-sources/" + sourceId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getSourceForNonExistentIdReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(get("/api/crawl-sources/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- PUT /api/crawl-sources/{id} ----------

    @Test
    void curatorCanUpdateOwnSource() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        Map<String, Object> body = Map.of(
                "name", "source-api-test-updated",
                "baseUrl", "https://example.org/updated",
                "description", "Updated description",
                "rateLimitPerMinute", 45,
                "requiresAuth", false);

        mockMvc.perform(put("/api/crawl-sources/" + sourceId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Updated description"));

        // Restore original name so subsequent test runs are consistent
        jdbcTemplate.update("UPDATE crawl_source_profiles SET name = 'source-api-test' WHERE id = ?", sourceId);
    }

    @Test
    void unauthenticatedCannotUpdateSource() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "unauthorized-update",
                "baseUrl", "https://example.org",
                "description", "Should fail",
                "rateLimitPerMinute", 10,
                "requiresAuth", false);

        mockMvc.perform(put("/api/crawl-sources/" + sourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotUpdateSource() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        Map<String, Object> body = Map.of(
                "name", "student-hijack",
                "baseUrl", "https://example.org",
                "description", "Should fail",
                "rateLimitPerMinute", 10,
                "requiresAuth", false);

        mockMvc.perform(put("/api/crawl-sources/" + sourceId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateNonExistentSourceReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        Map<String, Object> body = Map.of(
                "name", "ghost-source",
                "baseUrl", "https://example.org/ghost",
                "description", "Ghost source",
                "rateLimitPerMinute", 10,
                "requiresAuth", false);

        mockMvc.perform(put("/api/crawl-sources/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken())
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
