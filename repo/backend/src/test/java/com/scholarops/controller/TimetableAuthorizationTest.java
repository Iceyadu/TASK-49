package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.controller.support.AbstractWebMvcControllerTest;
import com.scholarops.model.dto.ScheduleRequest;
import com.scholarops.model.entity.Schedule;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.ScheduleService;
import com.scholarops.service.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.scholarops.controller.support.WebMvcTestUsers.userDetails;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests timetable and schedule authorization: ownership enforcement,
 * role validation, and unauthorized mutation attempts.
 */
@WebMvcTest(
        value = {TimetableController.class, ScheduleController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class TimetableAuthorizationTest extends AbstractWebMvcControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TimetableService timetableService;
    @MockBean private ScheduleService scheduleService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void grantAllPermissions() {
        grantAllEvaluatorPermissions();
    }

    @Nested
    @DisplayName("Instructor cannot access timetable endpoints")
    class InstructorCannotAccessTimetable {

        @Test
        @DisplayName("Instructor cannot move a schedule entry")
        void instructorCannotMove() throws Exception {
            mockMvc.perform(post("/api/schedules/1/move")
                            .with(csrf())
                            .with(user(userDetails(20L, "inst", "INSTRUCTOR", "QUIZ_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scheduleId\":\"00000000-0000-0000-0000-000000000001\",\"newStartTime\":\"2024-06-01T09:00\",\"newEndTime\":\"2024-06-01T10:00\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Instructor cannot merge schedules")
        void instructorCannotMerge() throws Exception {
            mockMvc.perform(post("/api/schedules/merge")
                            .with(csrf())
                            .with(user(userDetails(20L, "inst", "INSTRUCTOR", "QUIZ_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scheduleIds\":[1,2]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Instructor cannot split schedules")
        void instructorCannotSplit() throws Exception {
            mockMvc.perform(post("/api/schedules/1/split")
                            .with(csrf())
                            .with(user(userDetails(20L, "inst", "INSTRUCTOR", "QUIZ_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"splitTime\":\"2024-06-01T09:30\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Instructor cannot view change journal")
        void instructorCannotViewChangeJournal() throws Exception {
            mockMvc.perform(get("/api/schedules/change-journal")
                            .with(user(userDetails(20L, "inst", "INSTRUCTOR", "QUIZ_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Instructor cannot undo timetable changes")
        void instructorCannotUndo() throws Exception {
            mockMvc.perform(post("/api/schedules/undo").with(csrf())
                            .with(user(userDetails(20L, "inst", "INSTRUCTOR", "QUIZ_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Instructor cannot redo timetable changes")
        void instructorCannotRedo() throws Exception {
            mockMvc.perform(post("/api/schedules/redo").with(csrf())
                            .with(user(userDetails(20L, "inst", "INSTRUCTOR", "QUIZ_MANAGE"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Admin cannot access timetable endpoints")
    class AdminCannotAccessTimetable {

        @Test
        @DisplayName("Admin cannot list schedules")
        void adminCannotListSchedules() throws Exception {
            mockMvc.perform(get("/api/schedules")
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin cannot create schedules")
        void adminCannotCreateSchedule() throws Exception {
            ScheduleRequest request = new ScheduleRequest();
            request.setTitle("Study");
            request.setStartTime(LocalDateTime.of(2024, 6, 1, 9, 0));
            request.setEndTime(LocalDateTime.of(2024, 6, 1, 10, 0));

            mockMvc.perform(post("/api/schedules")
                            .with(csrf())
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin cannot delete schedules")
        void adminCannotDeleteSchedule() throws Exception {
            mockMvc.perform(delete("/api/schedules/1").with(csrf())
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin cannot manage locked periods")
        void adminCannotManageLockedPeriods() throws Exception {
            mockMvc.perform(get("/api/locked-periods")
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin cannot create locked periods")
        void adminCannotCreateLockedPeriod() throws Exception {
            mockMvc.perform(post("/api/locked-periods")
                            .with(csrf())
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Finals\",\"startTime\":\"2024-06-01T08:00\",\"endTime\":\"2024-06-01T17:00\",\"reason\":\"Exam\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin cannot delete locked periods")
        void adminCannotDeleteLockedPeriod() throws Exception {
            mockMvc.perform(delete("/api/locked-periods/1").with(csrf())
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("TA cannot access timetable endpoints")
    class TACannotAccessTimetable {

        @Test
        @DisplayName("TA cannot move schedules")
        void taCannotMove() throws Exception {
            mockMvc.perform(post("/api/schedules/1/move")
                            .with(csrf())
                            .with(user(userDetails(13L, "ta", "TEACHING_ASSISTANT", "GRADING_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scheduleId\":\"00000000-0000-0000-0000-000000000001\",\"newStartTime\":\"2024-06-01T09:00\",\"newEndTime\":\"2024-06-01T10:00\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TA cannot create schedules")
        void taCannotCreateSchedule() throws Exception {
            ScheduleRequest request = new ScheduleRequest();
            request.setTitle("Study");
            request.setStartTime(LocalDateTime.of(2024, 6, 1, 9, 0));
            request.setEndTime(LocalDateTime.of(2024, 6, 1, 10, 0));

            mockMvc.perform(post("/api/schedules")
                            .with(csrf())
                            .with(user(userDetails(13L, "ta", "TEACHING_ASSISTANT", "GRADING_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Student can access own timetable")
    class StudentCanAccessOwnTimetable {

        @Test
        @DisplayName("Student can list own schedules")
        void studentCanListSchedules() throws Exception {
            Schedule schedule = Schedule.builder()
                    .id(1L).title("Study Math")
                    .startTime(LocalDateTime.of(2024, 6, 1, 9, 0))
                    .endTime(LocalDateTime.of(2024, 6, 1, 10, 0)).build();

            when(scheduleService.getSchedules(any(), any(), any())).thenReturn(List.of(schedule));

            mockMvc.perform(get("/api/schedules")
                            .with(user(userDetails(11L, "stu", "STUDENT", "SCHEDULE_MANAGE_OWN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Student can create a schedule")
        void studentCanCreateSchedule() throws Exception {
            ScheduleRequest request = new ScheduleRequest();
            request.setTitle("Study Session");
            request.setStartTime(LocalDateTime.of(2024, 6, 1, 9, 0));
            request.setEndTime(LocalDateTime.of(2024, 6, 1, 10, 0));

            Schedule created = Schedule.builder()
                    .id(1L).title("Study Session")
                    .startTime(LocalDateTime.of(2024, 6, 1, 9, 0))
                    .endTime(LocalDateTime.of(2024, 6, 1, 10, 0)).build();

            when(scheduleService.createSchedule(any(), any())).thenReturn(created);

            mockMvc.perform(post("/api/schedules")
                            .with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "SCHEDULE_MANAGE_OWN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("Study Session"));
        }

        @Test
        @DisplayName("Student can move a schedule entry")
        void studentCanMoveSchedule() throws Exception {
            Schedule moved = Schedule.builder()
                    .id(1L).title("Moved")
                    .startTime(LocalDateTime.of(2024, 6, 1, 10, 0))
                    .endTime(LocalDateTime.of(2024, 6, 1, 11, 0)).build();

            when(timetableService.moveSchedule(eq(1L), any(), any(), any())).thenReturn(moved);

            mockMvc.perform(post("/api/schedules/1/move")
                            .with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "SCHEDULE_MANAGE_OWN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scheduleId\":\"00000000-0000-0000-0000-000000000001\",\"newStartTime\":\"2024-06-01T10:00\",\"newEndTime\":\"2024-06-01T11:00\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Moved"));
        }

        @Test
        @DisplayName("Student can view change journal")
        void studentCanViewChangeJournal() throws Exception {
            when(timetableService.getChangeJournal(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/schedules/change-journal")
                            .with(user(userDetails(11L, "stu", "STUDENT", "SCHEDULE_MANAGE_OWN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Student can undo timetable changes")
        void studentCanUndo() throws Exception {
            Schedule undone = Schedule.builder().id(1L).title("Original").build();
            when(timetableService.undo(any())).thenReturn(undone);

            mockMvc.perform(post("/api/schedules/undo").with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "SCHEDULE_MANAGE_OWN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Original"));
        }

        @Test
        @DisplayName("Student can redo timetable changes")
        void studentCanRedo() throws Exception {
            Schedule redone = Schedule.builder().id(1L).title("Redone").build();
            when(timetableService.redo(any())).thenReturn(redone);

            mockMvc.perform(post("/api/schedules/redo").with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "SCHEDULE_MANAGE_OWN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Redone"));
        }

        @Test
        @DisplayName("Student can view locked periods")
        void studentCanViewLockedPeriods() throws Exception {
            when(timetableService.getLockedPeriods(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/locked-periods")
                            .with(user(userDetails(11L, "stu", "STUDENT", "SCHEDULE_MANAGE_OWN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Student can delete own schedule")
        void studentCanDeleteSchedule() throws Exception {
            mockMvc.perform(delete("/api/schedules/1").with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "SCHEDULE_MANAGE_OWN"))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Content curator cannot access timetable endpoints")
    class CuratorCannotAccessTimetable {

        @Test
        @DisplayName("Curator cannot list schedules")
        void curatorCannotListSchedules() throws Exception {
            mockMvc.perform(get("/api/schedules")
                            .with(user(userDetails(8L, "cur", "CONTENT_CURATOR", "CONTENT_REVIEW"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Curator cannot merge schedules")
        void curatorCannotMerge() throws Exception {
            mockMvc.perform(post("/api/schedules/merge")
                            .with(csrf())
                            .with(user(userDetails(8L, "cur", "CONTENT_CURATOR", "CONTENT_REVIEW")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scheduleIds\":[1,2]}"))
                    .andExpect(status().isForbidden());
        }
    }
}
