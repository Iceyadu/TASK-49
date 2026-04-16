package com.scholarops.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.CrawlRuleRequest;
import com.scholarops.model.entity.Role;
import com.scholarops.model.entity.User;
import com.scholarops.repository.RoleRepository;
import com.scholarops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CrawlWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void ensureCuratorRoleUserExists() {
        User curator = userRepository.findByUsername("curator.integration")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("curator.integration")
                        .email("curator.integration@scholarops.local")
                        .fullName("Curator Integration User")
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
    }

    @Test
    @DisplayName("Unauthenticated caller cannot create crawl sources")
    void unauthenticatedCannotCreateSource() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "E2E Source",
                "baseUrl", "https://example.org",
                "description", "Boundary test source",
                "rateLimitPerMinute", 30,
                "requiresAuth", false);

        mockMvc.perform(post("/api/crawl-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Curator can create source and rule through HTTP boundary")
    void curatorCanCreateSourceAndRules() throws Exception {
        String accessToken = loginAndExtractAccessToken("curator.integration", "Curator@12345");

        Map<String, Object> sourceBody = Map.of(
                "name", "integration-source-" + System.currentTimeMillis(),
                "baseUrl", "https://example.org",
                "description", "Created from integration boundary test",
                "rateLimitPerMinute", 45,
                "requiresAuth", false);

        MvcResult createSourceResult = mockMvc.perform(post("/api/crawl-sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sourceBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode sourceJson = objectMapper.readTree(createSourceResult.getResponse().getContentAsString());
        long sourceId = sourceJson.path("data").path("id").asLong();

        CrawlRuleRequest ruleRequest = CrawlRuleRequest.builder()
                .extractionMethod("CSS_SELECTOR")
                .ruleDefinition(Map.of("title", "h1.article-title"))
                .fieldMappings(Map.of("title", "articleTitle"))
                .notes("Initial rule for integration workflow")
                .build();

        mockMvc.perform(post("/api/crawl-sources/" + sourceId + "/rules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.versionNumber").value(1));

        mockMvc.perform(get("/api/crawl-sources/" + sourceId + "/rules")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].versionNumber").value(1));
    }

    @Test
    @DisplayName("Student role cannot mutate crawl source boundaries")
    void studentCannotCreateSource() throws Exception {
        String accessToken = loginAndExtractAccessToken("student.integration", "Student@12345");
        Map<String, Object> sourceBody = Map.of(
                "name", "forbidden-source",
                "baseUrl", "https://example.org",
                "description", "Should be forbidden",
                "rateLimitPerMinute", 20,
                "requiresAuth", false);

        mockMvc.perform(post("/api/crawl-sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sourceBody)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    private String loginAndExtractAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}
