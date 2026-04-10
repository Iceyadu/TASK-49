package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.QuizAssemblyRequest;
import com.scholarops.model.entity.QuizPaper;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.QuizAssemblyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = QuizController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class QuizControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private QuizAssemblyService quizAssemblyService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void testAssembleQuiz() throws Exception {
        QuizAssemblyRequest request = new QuizAssemblyRequest();
        request.setQuestionBankId(1L);
        request.setTitle("Math Quiz");
        request.setTotalQuestions(10);
        request.setTimeLimitMinutes(60);

        QuizPaper quiz = QuizPaper.builder()
                .id(1L).title("Math Quiz").totalQuestions(10)
                .totalPoints(BigDecimal.TEN).isPublished(false).build();

        when(quizAssemblyService.assembleQuiz(any(), any())).thenReturn(quiz);

        mockMvc.perform(post("/api/quizzes/assemble")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Math Quiz"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void testPublishQuiz() throws Exception {
        QuizPaper quiz = QuizPaper.builder()
                .id(1L).title("Math Quiz").isPublished(true).build();

        when(quizAssemblyService.publishQuiz(any(), any())).thenReturn(quiz);

        mockMvc.perform(put("/api/quizzes/1/publish")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublished").value(true));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testUnauthorized() throws Exception {
        QuizAssemblyRequest request = new QuizAssemblyRequest();
        request.setQuestionBankId(1L);
        request.setTitle("Quiz");
        request.setTotalQuestions(5);

        mockMvc.perform(post("/api/quizzes/assemble")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
