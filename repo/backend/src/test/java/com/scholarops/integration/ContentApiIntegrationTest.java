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
 * Real API boundary tests for GET /api/content/{id} and
 * POST /api/content/publish-batch. Uses a live Spring context and MySQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContentApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long contentRecordId;

    @BeforeEach
    void ensureTestDataExists() {
        // Ensure curator user with CONTENT_CURATOR role
        User curator = userRepository.findByUsername("curator.content.api")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("curator.content.api")
                        .email("curator.content.api@scholarops.local")
                        .fullName("Content API Curator")
                        .passwordHash(passwordEncoder.encode("Curator@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));

        Role curatorRole = roleRepository.findByName("CONTENT_CURATOR")
                .orElseThrow(() -> new IllegalStateException("CONTENT_CURATOR role not found"));
        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, curator.getId(), curatorRole.getId());
        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    curator.getId(), curatorRole.getId());
        }

        // Ensure a standardized content record
        List<Long> contentIds = jdbcTemplate.queryForList(
                "SELECT id FROM standardized_content_records WHERE source_url = 'https://example.org/content-api-test' LIMIT 1",
                Long.class);
        if (contentIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO standardized_content_records " +
                    "(source_url, content_type, normalized_text, title, language, is_published, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    "https://example.org/content-api-test",
                    "ARTICLE",
                    "Test content body for API integration testing.",
                    "Content API Test Record",
                    "en",
                    false);
            contentRecordId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            contentRecordId = contentIds.get(0);
        }
    }

    // ---------- GET /api/content/{id} ----------

    @Test
    void curatorCanGetContentById() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.content.api", "Curator@12345");

        mockMvc.perform(get("/api/content/" + contentRecordId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(contentRecordId));
    }

    @Test
    void unauthenticatedCannotGetContent() throws Exception {
        mockMvc.perform(get("/api/content/" + contentRecordId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotGetContent() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        mockMvc.perform(get("/api/content/" + contentRecordId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getContentForNonExistentIdReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.content.api", "Curator@12345");

        mockMvc.perform(get("/api/content/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- POST /api/content/publish-batch ----------

    @Test
    void curatorCanPublishBatch() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.content.api", "Curator@12345");

        Map<String, Object> body = Map.of("contentRecordIds", List.of(contentRecordId));

        mockMvc.perform(post("/api/content/publish-batch")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unauthenticatedCannotPublishBatch() throws Exception {
        Map<String, Object> body = Map.of("contentRecordIds", List.of(contentRecordId));

        mockMvc.perform(post("/api/content/publish-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotPublishBatch() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        Map<String, Object> body = Map.of("contentRecordIds", List.of(contentRecordId));

        mockMvc.perform(post("/api/content/publish-batch")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void publishBatchWithEmptyListReturns400() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.content.api", "Curator@12345");

        Map<String, Object> body = Map.of("contentRecordIds", List.of());

        mockMvc.perform(post("/api/content/publish-batch")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
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
