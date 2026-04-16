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
 * Real API boundary tests for the submission lifecycle.
 * No mocks - uses a real Spring context and a live MySQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubmissionApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String ADMIN_PASSWORD = "Admin@12345678";
    private Long quizPaperId;

    @BeforeEach
    void ensureTestDataExists() {
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE username = 'admin'",
                passwordEncoder.encode(ADMIN_PASSWORD));

        // Ensure dedicated student user exists with STUDENT role
        User student = userRepository.findByUsername("student.submission.api")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("student.submission.api")
                        .email("student.submission.api@scholarops.local")
                        .fullName("Submission API Test Student")
                        .passwordHash(passwordEncoder.encode("Student@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found in seed data"));
        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, student.getId(), studentRole.getId());
        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    student.getId(), studentRole.getId());
        }

        // Ensure a question bank exists for the test quiz
        List<Long> bankIds = jdbcTemplate.queryForList(
                "SELECT id FROM question_banks WHERE name = 'submission-api-test-bank' LIMIT 1", Long.class);
        Long bankId;
        if (bankIds.isEmpty()) {
            Long adminId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE username = 'admin'", Long.class);
            jdbcTemplate.update(
                    "INSERT INTO question_banks (name, description, subject, created_by) VALUES (?, ?, ?, ?)",
                    "submission-api-test-bank", "Integration test question bank", "Testing", adminId);
            bankId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            bankId = bankIds.get(0);
        }

        // Ensure a published quiz paper exists with high max_attempts so repeated test runs don't exhaust it
        List<Long> quizIds = jdbcTemplate.queryForList(
                "SELECT id FROM quiz_papers WHERE title = 'Submission API Test Quiz' LIMIT 1", Long.class);
        if (quizIds.isEmpty()) {
            Long adminId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE username = 'admin'", Long.class);
            jdbcTemplate.update(
                    "INSERT INTO quiz_papers (title, question_bank_id, total_questions, total_points, " +
                    "max_attempts, is_published, time_limit_minutes, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    "Submission API Test Quiz", bankId, 5, 50.0, 9999, true, 60, adminId);
            quizPaperId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            quizPaperId = quizIds.get(0);
        }
    }

    @Test
    void unauthenticatedCannotStartSubmission() throws Exception {
        mockMvc.perform(post("/api/quizzes/" + quizPaperId + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startingSubmissionForNonExistentQuizReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.submission.api", "Student@12345");

        mockMvc.perform(post("/api/quizzes/999999999/submissions")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void nonStudentRoleCannotStartSubmission() throws Exception {
        // Admin has ADMINISTRATOR role but not STUDENT, so QUIZ_TAKE permission check fails
        AuthTokens adminTokens = loginAndExtractTokens("admin", ADMIN_PASSWORD);

        mockMvc.perform(post("/api/quizzes/" + quizPaperId + "/submissions")
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void studentCanStartSubmissionAndLifecycleProceedsToSubmit() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.submission.api", "Student@12345");

        // Start a new submission
        MvcResult startResult = mockMvc.perform(post("/api/quizzes/" + quizPaperId + "/submissions")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.timeRemainingSeconds").value(3600))
                .andReturn();

        JsonNode root = objectMapper.readTree(startResult.getResponse().getContentAsString());
        long submissionId = root.path("data").path("id").asLong();

        // Autosave with empty answers and updated time remaining
        Map<String, Object> autosaveBody = Map.of(
                "answers", List.of(),
                "timeRemainingSeconds", 3000);
        mockMvc.perform(put("/api/submissions/" + submissionId + "/autosave")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autosaveBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Final submit
        mockMvc.perform(put("/api/submissions/" + submissionId + "/submit")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("AUTO_GRADED"))
                .andExpect(jsonPath("$.data.submittedAt").exists());
    }

    @Test
    void submittingAlreadySubmittedSubmissionReturnsConflict() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.submission.api", "Student@12345");

        // Start a fresh submission
        MvcResult startResult = mockMvc.perform(post("/api/quizzes/" + quizPaperId + "/submissions")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(startResult.getResponse().getContentAsString());
        long submissionId = root.path("data").path("id").asLong();

        // Submit once - should succeed
        mockMvc.perform(put("/api/submissions/" + submissionId + "/submit")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());

        // Submit again - should be rejected with 409 Conflict
        mockMvc.perform(put("/api/submissions/" + submissionId + "/submit")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void studentCannotAccessAnotherStudentsSubmission() throws Exception {
        // Create a second student
        User otherStudent = userRepository.findByUsername("student.submission.api.2")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("student.submission.api.2")
                        .email("student.submission.api.2@scholarops.local")
                        .fullName("Second Submission Test Student")
                        .passwordHash(passwordEncoder.encode("Student@12345"))
                        .enabled(true).accountLocked(false).build()));
        Role studentRole = roleRepository.findByName("STUDENT").orElseThrow();
        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, otherStudent.getId(), studentRole.getId());
        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    otherStudent.getId(), studentRole.getId());
        }

        // First student starts a submission
        AuthTokens firstTokens = loginAndExtractTokens("student.submission.api", "Student@12345");
        MvcResult startResult = mockMvc.perform(post("/api/quizzes/" + quizPaperId + "/submissions")
                        .header("Authorization", "Bearer " + firstTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(startResult.getResponse().getContentAsString());
        long submissionId = root.path("data").path("id").asLong();

        // Second student tries to autosave the first student's submission
        AuthTokens secondTokens = loginAndExtractTokens("student.submission.api.2", "Student@12345");
        Map<String, Object> autosaveBody = Map.of("answers", List.of(), "timeRemainingSeconds", 1800);
        mockMvc.perform(put("/api/submissions/" + submissionId + "/autosave")
                        .header("Authorization", "Bearer " + secondTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autosaveBody)))
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
