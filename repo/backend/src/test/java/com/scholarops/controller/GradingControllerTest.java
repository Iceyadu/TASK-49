package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.GradingRequest;
import com.scholarops.model.dto.RubricScoreRequest;
import com.scholarops.model.entity.GradingState;
import com.scholarops.model.entity.SubmissionAnswer;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.GradingWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = GradingController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class GradingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private GradingWorkflowService gradingWorkflowService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "TEACHING_ASSISTANT")
    void testGetQueue() throws Exception {
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        GradingState state = GradingState.builder()
                .id(1L).submissionAnswer(answer).status("PENDING").build();
        Page<GradingState> page = new PageImpl<>(List.of(state));

        when(gradingWorkflowService.getGradingQueue(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/grading/queue")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void testGradeItem() throws Exception {
        GradingRequest request = new GradingRequest();
        request.setScore(8.0);
        request.setFeedback("Good work");

        GradingState result = GradingState.builder()
                .id(1L).status("GRADED").score(BigDecimal.valueOf(8))
                .feedback("Good work").build();

        when(gradingWorkflowService.gradeItem(eq(1L), any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/grading/submissions/1/grade")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GRADED"));
    }

    @Test
    @WithMockUser(roles = "TEACHING_ASSISTANT")
    void testAddRubricScores() throws Exception {
        RubricScoreRequest score = new RubricScoreRequest();
        score.setCriterionName("Clarity");
        score.setMaxScore(5.0);
        score.setAwardedScore(4.0);

        GradingState result = GradingState.builder()
                .id(1L).status("GRADED").score(BigDecimal.valueOf(4)).build();

        when(gradingWorkflowService.addRubricScores(eq(1L), any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/grading/submissions/1/rubric-scores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(score))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GRADED"));
    }
}
