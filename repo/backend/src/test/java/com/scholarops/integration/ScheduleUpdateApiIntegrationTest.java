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
 * Real API boundary tests for PUT /api/schedules/{id}.
 * Verifies ownership enforcement and successful update path at the HTTP boundary.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleUpdateApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long scheduleId;
    private Long studentUserId;

    @BeforeEach
    void ensureTestDataExists() {
        User student = userRepository.findByUsername("student.schedule.update")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("student.schedule.update")
                        .email("student.schedule.update@scholarops.local")
                        .fullName("Schedule Update Test Student")
                        .passwordHash(passwordEncoder.encode("Student@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));
        studentUserId = student.getId();

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));
        Long roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, studentUserId, studentRole.getId());
        if (roleCount == null || roleCount == 0L) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) VALUES (?, ?, NULL, NOW())",
                    studentUserId, studentRole.getId());
        }

        List<Long> schedIds = jdbcTemplate.queryForList(
                "SELECT id FROM schedules WHERE user_id = ? AND title = 'Update API Test Schedule' LIMIT 1",
                Long.class, studentUserId);
        if (schedIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO schedules (user_id, title, start_time, end_time, is_recurring) VALUES (?, ?, ?, ?, ?)",
                    studentUserId, "Update API Test Schedule",
                    "2025-01-10 09:00:00", "2025-01-10 10:00:00", false);
            scheduleId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            scheduleId = schedIds.get(0);
        }
    }

    @Test
    void studentCanUpdateOwnSchedule() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.schedule.update", "Student@12345");

        Map<String, Object> body = Map.of(
                "title", "Updated Schedule Title",
                "startTime", "2025-01-10T10:00:00",
                "endTime", "2025-01-10T11:00:00",
                "isRecurring", false);

        mockMvc.perform(put("/api/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Schedule Title"));
    }

    @Test
    void studentCannotUpdateAnotherStudentsSchedule() throws Exception {
        AuthTokens otherTokens = loginAndExtractTokens("student.timetable.other", "Student@12345");

        Map<String, Object> body = Map.of(
                "title", "Hijacked Schedule",
                "startTime", "2025-01-10T10:00:00",
                "endTime", "2025-01-10T11:00:00",
                "isRecurring", false);

        mockMvc.perform(put("/api/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + otherTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unauthenticatedCannotUpdateSchedule() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Unauthorized Update",
                "startTime", "2025-01-10T10:00:00",
                "endTime", "2025-01-10T11:00:00",
                "isRecurring", false);

        mockMvc.perform(put("/api/schedules/" + scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateNonExistentScheduleReturns404() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.schedule.update", "Student@12345");

        Map<String, Object> body = Map.of(
                "title", "Ghost Schedule",
                "startTime", "2025-01-10T10:00:00",
                "endTime", "2025-01-10T11:00:00",
                "isRecurring", false);

        mockMvc.perform(put("/api/schedules/999999999")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateIntoLockedPeriodReturnsConflict() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.schedule.update", "Student@12345");

        jdbcTemplate.update(
                "INSERT INTO locked_periods (user_id, title, start_time, end_time, reason) VALUES (?, ?, ?, ?, ?)",
                studentUserId, "Update Test Lock",
                "2025-03-01 08:00:00", "2025-03-01 18:00:00", "Exam day");

        Map<String, Object> body = Map.of(
                "title", "Conflict Schedule",
                "startTime", "2025-03-01T09:00:00",
                "endTime", "2025-03-01T10:00:00",
                "isRecurring", false);

        mockMvc.perform(put("/api/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
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
