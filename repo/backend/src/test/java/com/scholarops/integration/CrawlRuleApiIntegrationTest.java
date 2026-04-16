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
 * Real API boundary tests for crawl rule management endpoints:
 * GET /api/crawl-rules/{id}, POST /api/crawl-rules/{id}/revert/{versionId},
 * POST /api/crawl-rules/test-extraction.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrawlRuleApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long curatorUserId;
    private Long sourceId;
    private Long ruleV1Id;
    private Long ruleV2Id;

    @BeforeEach
    void ensureTestDataExists() {
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

        // Ensure a crawl source for rule tests
        List<Long> sourceIds = jdbcTemplate.queryForList(
                "SELECT id FROM crawl_source_profiles WHERE name = 'crawl-rule-test-source' AND created_by = ? LIMIT 1",
                Long.class, curatorUserId);
        if (sourceIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO crawl_source_profiles " +
                    "(name, base_url, description, rate_limit_per_minute, requires_auth, enabled, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    "crawl-rule-test-source", "https://example.org/rule-test",
                    "Source for crawl rule API tests", 10, false, true, curatorUserId);
            sourceId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            sourceId = sourceIds.get(0);
        }

        // Ensure version 1 (inactive after v2 is added)
        List<Long> v1Ids = jdbcTemplate.queryForList(
                "SELECT id FROM crawl_rule_versions WHERE source_profile_id = ? AND version_number = 1 LIMIT 1",
                Long.class, sourceId);
        if (v1Ids.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO crawl_rule_versions " +
                    "(source_profile_id, version_number, extraction_method, rule_definition, field_mappings, " +
                    "is_active, notes, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    sourceId, 1, "CSS_SELECTOR",
                    "{\"title\":\"h1\"}",
                    "{\"title\":\"articleTitle\"}",
                    false, "Version 1 for revert test", curatorUserId);
            ruleV1Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            ruleV1Id = v1Ids.get(0);
        }

        // Ensure version 2 (active)
        List<Long> v2Ids = jdbcTemplate.queryForList(
                "SELECT id FROM crawl_rule_versions WHERE source_profile_id = ? AND version_number = 2 LIMIT 1",
                Long.class, sourceId);
        if (v2Ids.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO crawl_rule_versions " +
                    "(source_profile_id, version_number, extraction_method, rule_definition, field_mappings, " +
                    "is_active, notes, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    sourceId, 2, "CSS_SELECTOR",
                    "{\"title\":\"h1\",\"body\":\".content\"}",
                    "{\"title\":\"articleTitle\",\"body\":\"bodyText\"}",
                    true, "Version 2 for revert test", curatorUserId);
            ruleV2Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            ruleV2Id = v2Ids.get(0);
        }
    }

    // ---------- GET /api/crawl-rules/{id} ----------

    @Test
    void curatorCanGetRuleById() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(get("/api/crawl-rules/" + ruleV1Id)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(ruleV1Id));
    }

    @Test
    void unauthenticatedCannotGetRule() throws Exception {
        mockMvc.perform(get("/api/crawl-rules/" + ruleV1Id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotGetRule() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        mockMvc.perform(get("/api/crawl-rules/" + ruleV1Id)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRuleForNonExistentIdReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(get("/api/crawl-rules/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- POST /api/crawl-rules/{id}/revert/{versionId} ----------

    @Test
    void curatorCanRevertRuleToEarlierVersion() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        // Revert current active rule (v2) back to v1's definition
        mockMvc.perform(post("/api/crawl-rules/" + ruleV2Id + "/revert/" + ruleV1Id)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notes").value("Reverted to version 1"));
    }

    @Test
    void unauthenticatedCannotRevertRule() throws Exception {
        mockMvc.perform(post("/api/crawl-rules/" + ruleV2Id + "/revert/" + ruleV1Id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revertNonExistentRuleReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(post("/api/crawl-rules/999999999/revert/" + ruleV1Id)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- POST /api/crawl-rules/test-extraction ----------

    @Test
    void unauthenticatedCannotTestExtraction() throws Exception {
        Map<String, Object> body = Map.of(
                "sampleUrl", "https://example.org",
                "extractionMethod", "CSS_SELECTOR",
                "ruleDefinition", Map.of("title", "h1"));

        mockMvc.perform(post("/api/crawl-rules/test-extraction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotTestExtraction() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        Map<String, Object> body = Map.of(
                "sampleUrl", "https://example.org",
                "extractionMethod", "CSS_SELECTOR",
                "ruleDefinition", Map.of("title", "h1"));

        mockMvc.perform(post("/api/crawl-rules/test-extraction")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testExtractionWithMissingRequiredFieldReturns400() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        // sampleUrl is @NotBlank — omitting it causes validation failure before service call
        Map<String, Object> body = Map.of(
                "extractionMethod", "CSS_SELECTOR",
                "ruleDefinition", Map.of("title", "h1"));

        mockMvc.perform(post("/api/crawl-rules/test-extraction")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testExtractionWithUnreachableUrlReturns4xx() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        // Auth passes; service-layer URL fetch failure produces a 4xx error
        Map<String, Object> body = Map.of(
                "sampleUrl", "http://localhost:0/unreachable",
                "extractionMethod", "CSS_SELECTOR",
                "ruleDefinition", Map.of("title", "h1"));

        mockMvc.perform(post("/api/crawl-rules/test-extraction")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
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
