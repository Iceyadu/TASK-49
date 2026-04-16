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
 * Real API boundary tests for question bank and question management endpoints:
 * GET /api/question-banks/{id}, PUT /api/questions/{id},
 * DELETE /api/questions/{id}, POST /api/knowledge-tags.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuestionBankApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long instructorId;
    private Long bankId;
    private Long questionId;

    @BeforeEach
    void ensureTestDataExists() {
        // Ensure instructor user with INSTRUCTOR role
        User instructor = userRepository.findByUsername("instructor.qbank.api")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("instructor.qbank.api")
                        .email("instructor.qbank.api@scholarops.local")
                        .fullName("QuestionBank API Instructor")
                        .passwordHash(passwordEncoder.encode("Instructor@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));
        instructorId = instructor.getId();

        Role instructorRole = roleRepository.findByName("INSTRUCTOR")
                .orElseThrow(() -> new IllegalStateException("INSTRUCTOR role not found"));
        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, instructorId, instructorRole.getId());
        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    instructorId, instructorRole.getId());
        }

        // Ensure question bank owned by instructor
        List<Long> bankIds = jdbcTemplate.queryForList(
                "SELECT id FROM question_banks WHERE name = 'qbank-api-test-bank' AND created_by = ? LIMIT 1",
                Long.class, instructorId);
        if (bankIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO question_banks (name, description, subject, created_by) VALUES (?, ?, ?, ?)",
                    "qbank-api-test-bank", "API test bank", "Testing", instructorId);
            bankId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            bankId = bankIds.get(0);
        }

        // Ensure a question in the bank owned by instructor
        List<Long> qIds = jdbcTemplate.queryForList(
                "SELECT id FROM questions WHERE question_bank_id = ? AND created_by = ? AND question_text = 'What is 2+2?' LIMIT 1",
                Long.class, bankId, instructorId);
        if (qIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO questions (question_bank_id, question_type, difficulty_level, question_text, " +
                    "correct_answer, points, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    bankId, "MULTIPLE_CHOICE", 1, "What is 2+2?", "4", 1.0, instructorId);
            questionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            questionId = qIds.get(0);
        }
    }

    // ---------- GET /api/question-banks/{id} ----------

    @Test
    void instructorCanGetBankById() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("instructor.qbank.api", "Instructor@12345");

        mockMvc.perform(get("/api/question-banks/" + bankId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(bankId))
                .andExpect(jsonPath("$.data.name").value("qbank-api-test-bank"));
    }

    @Test
    void unauthenticatedCannotGetBank() throws Exception {
        mockMvc.perform(get("/api/question-banks/" + bankId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotGetBank() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        mockMvc.perform(get("/api/question-banks/" + bankId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getBankForNonExistentIdReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("instructor.qbank.api", "Instructor@12345");

        mockMvc.perform(get("/api/question-banks/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- PUT /api/questions/{id} ----------

    @Test
    void instructorCanUpdateQuestion() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("instructor.qbank.api", "Instructor@12345");

        Map<String, Object> body = Map.of(
                "questionBankId", bankId,
                "questionType", "MULTIPLE_CHOICE",
                "difficultyLevel", 2,
                "questionText", "What is 3+3?",
                "correctAnswer", "6",
                "points", 2.0,
                "options", List.of("3", "5", "6", "9"));

        mockMvc.perform(put("/api/questions/" + questionId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questionText").value("What is 3+3?"));
    }

    @Test
    void unauthenticatedCannotUpdateQuestion() throws Exception {
        Map<String, Object> body = Map.of(
                "questionBankId", bankId,
                "questionType", "MULTIPLE_CHOICE",
                "difficultyLevel", 1,
                "questionText", "What is 2+2?",
                "correctAnswer", "4");

        mockMvc.perform(put("/api/questions/" + questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateNonExistentQuestionReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("instructor.qbank.api", "Instructor@12345");

        Map<String, Object> body = Map.of(
                "questionBankId", bankId,
                "questionType", "MULTIPLE_CHOICE",
                "difficultyLevel", 1,
                "questionText", "Ghost question?",
                "correctAnswer", "none");

        mockMvc.perform(put("/api/questions/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- DELETE /api/questions/{id} ----------

    @Test
    void instructorCanDeleteOwnQuestion() throws Exception {
        // Insert a disposable question to delete
        jdbcTemplate.update(
                "INSERT INTO questions (question_bank_id, question_type, difficulty_level, question_text, " +
                "correct_answer, points, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                bankId, "SHORT_ANSWER", 1, "Disposable question for delete test?", "answer", 1.0, instructorId);
        Long disposableId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        AuthTokens tokens = loginAndExtractTokens("instructor.qbank.api", "Instructor@12345");

        mockMvc.perform(delete("/api/questions/" + disposableId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unauthenticatedCannotDeleteQuestion() throws Exception {
        mockMvc.perform(delete("/api/questions/" + questionId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteNonExistentQuestionReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("instructor.qbank.api", "Instructor@12345");

        mockMvc.perform(delete("/api/questions/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------- POST /api/knowledge-tags ----------

    @Test
    void instructorCanCreateKnowledgeTag() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("instructor.qbank.api", "Instructor@12345");

        Map<String, Object> body = Map.of(
                "name", "algebra-" + System.currentTimeMillis(),
                "category", "Mathematics");

        mockMvc.perform(post("/api/knowledge-tags")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("Mathematics"));
    }

    @Test
    void unauthenticatedCannotCreateKnowledgeTag() throws Exception {
        Map<String, Object> body = Map.of("name", "test-tag", "category", "Testing");

        mockMvc.perform(post("/api/knowledge-tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotCreateKnowledgeTag() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.integration", "Student@12345");

        Map<String, Object> body = Map.of("name", "student-tag", "category", "Unauthorized");

        mockMvc.perform(post("/api/knowledge-tags")
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
