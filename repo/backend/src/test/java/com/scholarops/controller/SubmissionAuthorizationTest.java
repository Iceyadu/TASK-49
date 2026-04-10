package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.AutosaveRequest;
import com.scholarops.model.dto.GradingRequest;
import com.scholarops.model.dto.RubricScoreRequest;
import com.scholarops.model.entity.GradingState;
import com.scholarops.model.entity.Submission;
import com.scholarops.model.entity.SubmissionAnswer;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.GradingWorkflowService;
import com.scholarops.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
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

/**
 * Tests submission ownership enforcement and cross-role authorization boundaries
 * around submissions and grading.
 */
@WebMvcTest(
        value = {SubmissionController.class, GradingController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class SubmissionAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SubmissionService submissionService;
    @MockBean private GradingWorkflowService gradingWorkflowService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private PermissionEvaluator permissionEvaluator;

    @BeforeEach
    void grantAllPermissions() {
        when(permissionEvaluator.hasPermission(any(Authentication.class), any(), any()))
                .thenReturn(true);
    }

    // -----------------------------------------------------------------------
    // Instructor cannot take quizzes (student-only submission endpoints)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Instructor cannot perform student submission actions")
    class InstructorCannotTakeQuizzes {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot start a quiz submission")
        void instructorCannotStartSubmission() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot autosave a submission")
        void instructorCannotAutosave() throws Exception {
            AutosaveRequest request = new AutosaveRequest();
            request.setTimeRemainingSeconds(1800);
            request.setAnswers(List.of());

            mockMvc.perform(put("/api/submissions/1/autosave")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot submit a quiz")
        void instructorCannotSubmit() throws Exception {
            mockMvc.perform(put("/api/submissions/1/submit").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot view own feedback (student endpoint)")
        void instructorCannotViewFeedback() throws Exception {
            mockMvc.perform(get("/api/submissions/1/feedback"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // Teaching assistant cannot take quizzes
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Teaching assistant cannot perform student submission actions")
    class TACannotTakeQuizzes {

        @Test
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA cannot start a quiz submission")
        void taCannotStartSubmission() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA cannot submit a quiz")
        void taCannotSubmit() throws Exception {
            mockMvc.perform(put("/api/submissions/1/submit").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // Student cannot access grading endpoints
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Student cannot access grading endpoints")
    class StudentCannotGrade {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot view grading queue")
        void studentCannotViewGradingQueue() throws Exception {
            mockMvc.perform(get("/api/grading/queue"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot view grading state")
        void studentCannotViewGradingState() throws Exception {
            mockMvc.perform(get("/api/grading/submissions/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot grade submissions")
        void studentCannotGradeSubmission() throws Exception {
            GradingRequest request = new GradingRequest();
            request.setScore(8.0);
            request.setFeedback("Good");

            mockMvc.perform(post("/api/grading/submissions/1/grade")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot add rubric scores")
        void studentCannotAddRubricScores() throws Exception {
            RubricScoreRequest score = new RubricScoreRequest();
            score.setCriterionName("Clarity");
            score.setMaxScore(5.0);
            score.setAwardedScore(4.0);

            mockMvc.perform(post("/api/grading/submissions/1/rubric-scores")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(score))))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // Valid role + permission: student can start and submit
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Student can perform authorized submission actions")
    class StudentCanSubmit {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student can start a submission")
        void studentCanStartSubmission() throws Exception {
            Submission submission = Submission.builder()
                    .id(1L).attemptNumber(1).status("IN_PROGRESS")
                    .timeRemainingSeconds(3600).build();

            when(submissionService.startSubmission(eq(1L), any())).thenReturn(submission);

            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student can submit a quiz")
        void studentCanSubmitQuiz() throws Exception {
            Submission submission = Submission.builder()
                    .id(1L).status("SUBMITTED").build();

            when(submissionService.submitSubmission(eq(1L), any())).thenReturn(submission);

            mockMvc.perform(put("/api/submissions/1/submit").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        }
    }

    // -----------------------------------------------------------------------
    // Valid role + permission: instructor and TA can grade
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Authorized users can access grading")
    class AuthorizedGrading {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor can grade a submission")
        void instructorCanGrade() throws Exception {
            GradingRequest request = new GradingRequest();
            request.setScore(9.0);
            request.setFeedback("Excellent");

            GradingState result = GradingState.builder()
                    .id(1L).status("GRADED").score(BigDecimal.valueOf(9))
                    .feedback("Excellent").build();

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
        @DisplayName("TA can view grading queue")
        void taCanViewQueue() throws Exception {
            SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
            GradingState state = GradingState.builder()
                    .id(1L).submissionAnswer(answer).status("PENDING").build();
            Page<GradingState> page = new PageImpl<>(List.of(state));

            when(gradingWorkflowService.getGradingQueue(any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/grading/queue").param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA can add rubric scores")
        void taCanAddRubricScores() throws Exception {
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

    // -----------------------------------------------------------------------
    // Admin cannot access submission or grading endpoints
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Admin cannot access submission or grading endpoints")
    class AdminCannotAccessSubmissionsOrGrading {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot start submissions")
        void adminCannotStartSubmission() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot access grading queue")
        void adminCannotAccessGradingQueue() throws Exception {
            mockMvc.perform(get("/api/grading/queue"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot grade submissions")
        void adminCannotGrade() throws Exception {
            GradingRequest request = new GradingRequest();
            request.setScore(8.0);
            request.setFeedback("Good");

            mockMvc.perform(post("/api/grading/submissions/1/grade")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
