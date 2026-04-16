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
 * Real API boundary tests for timetable schedule operations.
 * Verifies ownership enforcement, locked-period conflict detection, and
 * merge/split constraints at the HTTP layer without mocking services.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TimetableApiIntegrationTest {

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
        // Ensure student user with STUDENT role (and SCHEDULE_MANAGE_OWN permission via role)
        User student = userRepository.findByUsername("student.timetable.api")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("student.timetable.api")
                        .email("student.timetable.api@scholarops.local")
                        .fullName("Timetable API Test Student")
                        .passwordHash(passwordEncoder.encode("Student@12345"))
                        .enabled(true)
                        .accountLocked(false)
                        .build()));
        studentUserId = student.getId();

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

        // Remove previously created test locks so each test starts clean.
        jdbcTemplate.update("DELETE FROM locked_periods WHERE user_id = ? AND title = ?",
                studentUserId, "Exam Block API Test");

        // Ensure a known schedule entry owned by this student
        List<Long> schedIds = jdbcTemplate.queryForList(
                "SELECT id FROM schedules WHERE user_id = ? AND title = 'API Move Test Schedule' LIMIT 1",
                Long.class, studentUserId);
        if (schedIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO schedules (user_id, title, start_time, end_time, is_recurring) VALUES (?, ?, ?, ?, ?)",
                    studentUserId, "API Move Test Schedule",
                    "2024-08-01 09:00:00", "2024-08-01 10:00:00", false);
            scheduleId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            scheduleId = schedIds.get(0);
            // Reset to known state for each test run
            jdbcTemplate.update(
                    "UPDATE schedules SET start_time = '2024-08-01 09:00:00', end_time = '2024-08-01 10:00:00' WHERE id = ?",
                    scheduleId);
        }
    }

    @Test
    void unauthenticatedCannotMoveSchedule() throws Exception {
        Map<String, Object> body = Map.of(
                "scheduleId", "00000000-0000-0000-0000-000000000001",
                "newStartTime", "2024-08-01T14:00:00",
                "newEndTime", "2024-08-01T15:00:00");

        mockMvc.perform(post("/api/schedules/" + scheduleId + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanMoveOwnSchedule() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.timetable.api", "Student@12345");

        Map<String, Object> body = Map.of(
                "scheduleId", "00000000-0000-0000-0000-000000000001",
                "newStartTime", "2024-08-01T14:00:00",
                "newEndTime", "2024-08-01T15:00:00");

        mockMvc.perform(post("/api/schedules/" + scheduleId + "/move")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void studentCannotMoveAnotherStudentsSchedule() throws Exception {
        // Create a second student who will attempt to move the first student's schedule
        User otherStudent = userRepository.findByUsername("student.timetable.other")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("student.timetable.other")
                        .email("student.timetable.other@scholarops.local")
                        .fullName("Other Timetable Test Student")
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

        AuthTokens otherTokens = loginAndExtractTokens("student.timetable.other", "Student@12345");

        Map<String, Object> body = Map.of(
                "scheduleId", "00000000-0000-0000-0000-000000000001",
                "newStartTime", "2024-08-01T16:00:00",
                "newEndTime", "2024-08-01T17:00:00");

        mockMvc.perform(post("/api/schedules/" + scheduleId + "/move")
                        .header("Authorization", "Bearer " + otherTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void movingScheduleIntoLockedPeriodReturnsConflict() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.timetable.api", "Student@12345");

        // Create a broad locked period to avoid timezone edge cases in DB/runtime conversions
        jdbcTemplate.update(
                "INSERT INTO locked_periods (user_id, title, start_time, end_time, reason) VALUES (?, ?, ?, ?, ?)",
                studentUserId, "Exam Block API Test",
                "2024-01-01 00:00:00", "2030-01-01 00:00:00", "Final examinations week");

        // Attempt to move the schedule into the locked period
        Map<String, Object> body = Map.of(
                "scheduleId", "00000000-0000-0000-0000-000000000001",
                "newStartTime", "2026-01-10T11:00:00",
                "newEndTime", "2026-01-10T12:00:00");

        mockMvc.perform(post("/api/schedules/" + scheduleId + "/move")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.conflictingPeriodTitle").value("Exam Block API Test"));
    }

    @Test
    void mergingFewerThanTwoSchedulesReturnsBadRequest() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.timetable.api", "Student@12345");

        // Attempt merge with only one schedule ID
        Map<String, Object> body = Map.of("scheduleIds", List.of(scheduleId));

        mockMvc.perform(post("/api/schedules/merge")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void studentCanSplitOwnSchedule() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.timetable.api", "Student@12345");

        // Normalize schedule window via API first to avoid DB timezone conversion edge cases.
        Map<String, Object> moveBody = Map.of(
                "scheduleId", "00000000-0000-0000-0000-000000000001",
                "newStartTime", "2024-08-10T09:00:00",
                "newEndTime", "2024-08-10T12:00:00");
        mockMvc.perform(post("/api/schedules/" + scheduleId + "/move")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Pick split time safely away from boundaries.
        Map<String, Object> body = Map.of("splitTime", "2024-08-10T10:30:00");

        mockMvc.perform(post("/api/schedules/" + scheduleId + "/split")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void changeJournalIsAccessibleToOwner() throws Exception {
        AuthTokens tokens = loginAndExtractTokens("student.timetable.api", "Student@12345");

        mockMvc.perform(get("/api/schedules/change-journal")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
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
