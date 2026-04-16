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
 * Real API boundary tests for the crawl-runs lifecycle:
 * POST /api/crawl-runs, GET /api/crawl-runs,
 * GET /api/crawl-runs/{id}, POST /api/crawl-runs/{id}/cancel.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrawlRunApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long curatorUserId;
    private Long sourceId;
    private Long ruleVersionId;

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

        // Ensure a crawl source for run tests
        List<Long> sourceIds = jdbcTemplate.queryForList(
                "SELECT id FROM crawl_source_profiles WHERE name = 'crawl-run-test-source' AND created_by = ? LIMIT 1",
                Long.class, curatorUserId);
        if (sourceIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO crawl_source_profiles " +
                    "(name, base_url, description, rate_limit_per_minute, requires_auth, enabled, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    "crawl-run-test-source", "https://example.org/run-test",
                    "Source for crawl run API tests", 10, false, true, curatorUserId);
            sourceId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            sourceId = sourceIds.get(0);
        }

        // Ensure a crawl rule version for this source
        List<Long> ruleIds = jdbcTemplate.queryForList(
                "SELECT id FROM crawl_rule_versions WHERE source_profile_id = ? AND version_number = 1 LIMIT 1",
                Long.class, sourceId);
        if (ruleIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO crawl_rule_versions " +
                    "(source_profile_id, version_number, extraction_method, rule_definition, field_mappings, " +
                    "is_active, notes, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    sourceId, 1, "CSS_SELECTOR",
                    "{\"title\":\"h1\"}",
                    "{\"title\":\"articleTitle\"}",
                    true, "Initial rule for run tests", curatorUserId);
            ruleVersionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            ruleVersionId = ruleIds.get(0);
        }
    }

    // ---------- POST /api/crawl-runs ----------

    @Test
    void unauthenticatedCannotStartCrawlRun() throws Exception {
        Map<String, Object> body = Map.of(
                "sourceProfileId", sourceId,
                "ruleVersionId", ruleVersionId);

        mockMvc.perform(post("/api/crawl-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotStartCrawlRun() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        Map<String, Object> body = Map.of(
                "sourceProfileId", sourceId,
                "ruleVersionId", ruleVersionId);

        mockMvc.perform(post("/api/crawl-runs")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void curatorCanStartCrawlRunAndReceivesCreatedStatus() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        Map<String, Object> body = Map.of(
                "sourceProfileId", sourceId,
                "ruleVersionId", ruleVersionId);

        mockMvc.perform(post("/api/crawl-runs")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void startCrawlRunWithNonExistentSourceReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        Map<String, Object> body = Map.of(
                "sourceProfileId", 999999999,
                "ruleVersionId", ruleVersionId);

        mockMvc.perform(post("/api/crawl-runs")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- GET /api/crawl-runs ----------

    @Test
    void curatorCanListCrawlRuns() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(get("/api/crawl-runs")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void curatorCanListCrawlRunsFilteredBySource() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(get("/api/crawl-runs?sourceId=" + sourceId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unauthenticatedCannotListCrawlRuns() throws Exception {
        mockMvc.perform(get("/api/crawl-runs"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET /api/crawl-runs/{id} ----------

    @Test
    void curatorCanGetCrawlRunById() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        // Start a run to get an ID
        Map<String, Object> startBody = Map.of(
                "sourceProfileId", sourceId,
                "ruleVersionId", ruleVersionId);
        MvcResult startResult = mockMvc.perform(post("/api/crawl-runs")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startBody)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(startResult.getResponse().getContentAsString());
        long runId = root.path("data").path("id").asLong();

        mockMvc.perform(get("/api/crawl-runs/" + runId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(runId));
    }

    @Test
    void getCrawlRunForNonExistentIdReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(get("/api/crawl-runs/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- POST /api/crawl-runs/{id}/cancel ----------

    @Test
    void curatorCanCancelPendingCrawlRun() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        // Start a run then cancel it
        Map<String, Object> startBody = Map.of(
                "sourceProfileId", sourceId,
                "ruleVersionId", ruleVersionId);
        MvcResult startResult = mockMvc.perform(post("/api/crawl-runs")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startBody)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(startResult.getResponse().getContentAsString());
        long runId = root.path("data").path("id").asLong();

        mockMvc.perform(post("/api/crawl-runs/" + runId + "/cancel")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unauthenticatedCannotCancelCrawlRun() throws Exception {
        mockMvc.perform(post("/api/crawl-runs/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelNonExistentCrawlRunReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("curator.integration", "Curator@12345");

        mockMvc.perform(post("/api/crawl-runs/999999999/cancel")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
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
