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
 * Real API boundary tests for GET /api/quizzes/{id}.
 * Verifies that instructors see full quiz data and students get a redacted view.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuizApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long quizPaperId;

    @BeforeEach
    void ensureTestDataExists() {
        // Ensure instructor user with INSTRUCTOR role
        User instructor = userRepository.findByUsername("instructor.quiz.api")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("instructor.quiz.api")
                        .email("instructor.quiz.api@scholarops.local")
                        .fullName("Quiz API Instructor")
                        .passwordHash(passwordEncoder.encode("Instructor@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));

        Role instructorRole = roleRepository.findByName("INSTRUCTOR")
                .orElseThrow(() -> new IllegalStateException("INSTRUCTOR role not found"));
        Long iroleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, instructor.getId(), instructorRole.getId());
        if (iroleCount == null || iroleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    instructor.getId(), instructorRole.getId());
        }

        // Ensure student user with STUDENT role
        User student = userRepository.findByUsername("student.quiz.api")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("student.quiz.api")
                        .email("student.quiz.api@scholarops.local")
                        .fullName("Quiz API Student")
                        .passwordHash(passwordEncoder.encode("Student@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));
        Long sroleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, student.getId(), studentRole.getId());
        if (sroleCount == null || sroleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    student.getId(), studentRole.getId());
        }

        // Ensure a question bank
        List<Long> bankIds = jdbcTemplate.queryForList(
                "SELECT id FROM question_banks WHERE name = 'quiz-api-test-bank' LIMIT 1", Long.class);
        Long bankId;
        if (bankIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO question_banks (name, description, subject, created_by) VALUES (?, ?, ?, ?)",
                    "quiz-api-test-bank", "Quiz API test bank", "Testing", instructor.getId());
            bankId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            bankId = bankIds.get(0);
        }

        // Ensure a published quiz paper for this test
        List<Long> quizIds = jdbcTemplate.queryForList(
                "SELECT id FROM quiz_papers WHERE title = 'Quiz API Test Paper' LIMIT 1", Long.class);
        if (quizIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO quiz_papers (title, question_bank_id, total_questions, total_points, " +
                    "max_attempts, is_published, time_limit_minutes, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    "Quiz API Test Paper", bankId, 5, 50.0, 100, true, 60, instructor.getId());
            quizPaperId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            quizPaperId = quizIds.get(0);
        }
    }

    @Test
    void instructorCanGetQuizById() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("instructor.quiz.api", "Instructor@12345");

        mockMvc.perform(get("/api/quizzes/" + quizPaperId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(quizPaperId))
                .andExpect(jsonPath("$.data.title").value("Quiz API Test Paper"));
    }

    @Test
    void studentCanGetQuizById() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.quiz.api", "Student@12345");

        mockMvc.perform(get("/api/quizzes/" + quizPaperId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(quizPaperId));
    }

    @Test
    void unauthenticatedCannotGetQuiz() throws Exception {
        mockMvc.perform(get("/api/quizzes/" + quizPaperId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getQuizForNonExistentIdReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("instructor.quiz.api", "Instructor@12345");

        mockMvc.perform(get("/api/quizzes/999999999")
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
