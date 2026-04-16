package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.QuizAssemblyRequest;
import com.scholarops.model.entity.QuizPaper;
import com.scholarops.controller.support.AbstractWebMvcControllerTest;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.QuizAssemblyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static com.scholarops.controller.support.WebMvcTestUsers.userDetails;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = QuizController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class QuizControllerTest extends AbstractWebMvcControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private QuizAssemblyService quizAssemblyService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void grantPerms() {
        grantAllEvaluatorPermissions();
    }

    @Test
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
                        .with(user(userDetails(5L, "instr", "INSTRUCTOR", "QUIZ_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Math Quiz"));
    }

    @Test
    void testPublishQuiz() throws Exception {
        QuizPaper quiz = QuizPaper.builder()
                .id(1L).title("Math Quiz").isPublished(true).build();

        when(quizAssemblyService.publishQuiz(any(), any())).thenReturn(quiz);

        mockMvc.perform(put("/api/quizzes/1/publish")
                        .with(csrf())
                        .with(user(userDetails(5L, "instr", "INSTRUCTOR", "QUIZ_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublished").value(true));
    }

    @Test
    void testUnauthorized() throws Exception {
        QuizAssemblyRequest request = new QuizAssemblyRequest();
        request.setQuestionBankId(1L);
        request.setTitle("Quiz");
        request.setTotalQuestions(5);

        mockMvc.perform(post("/api/quizzes/assemble")
                        .with(csrf())
                        .with(user(userDetails(6L, "stu", "STUDENT", "QUIZ_TAKE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
