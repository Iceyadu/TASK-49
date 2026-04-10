package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.exception.LockedPeriodConflictException;
import com.scholarops.model.dto.ScheduleRequest;
import com.scholarops.model.entity.Schedule;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.ScheduleService;
import com.scholarops.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ScheduleController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class ScheduleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ScheduleService scheduleService;
    @MockBean private TimetableService timetableService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "STUDENT")
    void testCreateSchedule() throws Exception {
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Study Session"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testLockedPeriodConflict409() throws Exception {
        ScheduleRequest request = new ScheduleRequest();
        request.setTitle("Study Session");
        request.setStartTime(LocalDateTime.of(2024, 6, 1, 9, 0));
        request.setEndTime(LocalDateTime.of(2024, 6, 1, 10, 0));

        when(scheduleService.createSchedule(any(), any())).thenThrow(
                new LockedPeriodConflictException(
                        "Conflicts with locked period",
                        1L, "Finals Week",
                        LocalDateTime.of(2024, 6, 1, 8, 0),
                        LocalDateTime.of(2024, 6, 1, 17, 0)));

        mockMvc.perform(post("/api/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
