package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.AutosaveRequest;
import com.scholarops.model.entity.Submission;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SubmissionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class SubmissionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SubmissionService submissionService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "STUDENT")
    void testStartSubmission() throws Exception {
        Submission submission = Submission.builder()
                .id(1L).attemptNumber(1).status("IN_PROGRESS")
                .timeRemainingSeconds(3600).build();

        when(submissionService.startSubmission(eq(1L), any())).thenReturn(submission);

        mockMvc.perform(post("/api/quizzes/1/submissions")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testAutosave() throws Exception {
        AutosaveRequest request = new AutosaveRequest();
        request.setTimeRemainingSeconds(1800);
        request.setAnswers(List.of());

        mockMvc.perform(put("/api/submissions/1/autosave")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testSubmit() throws Exception {
        Submission submission = Submission.builder()
                .id(1L).status("SUBMITTED").build();

        when(submissionService.submitSubmission(eq(1L), any())).thenReturn(submission);

        mockMvc.perform(put("/api/submissions/1/submit")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }
}
