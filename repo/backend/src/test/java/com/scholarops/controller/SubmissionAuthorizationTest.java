package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.controller.support.AbstractWebMvcControllerTest;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.scholarops.controller.support.WebMvcTestUsers.userDetails;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
class SubmissionAuthorizationTest extends AbstractWebMvcControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SubmissionService submissionService;
    @MockBean private GradingWorkflowService gradingWorkflowService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void grantAllPermissions() {
        grantAllEvaluatorPermissions();
    }

    @Nested
    @DisplayName("Instructor cannot perform student submission actions")
    class InstructorCannotTakeQuizzes {

        @Test
        @DisplayName("Instructor cannot start a quiz submission")
        void instructorCannotStartSubmission() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf())
                            .with(user(userDetails(20L, "instr", "INSTRUCTOR", "QUIZ_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Instructor cannot autosave a submission")
        void instructorCannotAutosave() throws Exception {
            AutosaveRequest request = new AutosaveRequest();
            request.setTimeRemainingSeconds(1800);
            request.setAnswers(List.of());

            mockMvc.perform(put("/api/submissions/1/autosave")
                            .with(csrf())
                            .with(user(userDetails(20L, "instr", "INSTRUCTOR", "QUIZ_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Instructor cannot submit a quiz")
        void instructorCannotSubmit() throws Exception {
            mockMvc.perform(put("/api/submissions/1/submit").with(csrf())
                            .with(user(userDetails(20L, "instr", "INSTRUCTOR", "QUIZ_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Instructor cannot view own feedback (student endpoint)")
        void instructorCannotViewFeedback() throws Exception {
            mockMvc.perform(get("/api/submissions/1/feedback")
                            .with(user(userDetails(20L, "instr", "INSTRUCTOR", "QUIZ_MANAGE"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Teaching assistant cannot perform student submission actions")
    class TACannotTakeQuizzes {

        @Test
        @DisplayName("TA cannot start a quiz submission")
        void taCannotStartSubmission() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf())
                            .with(user(userDetails(13L, "ta", "TEACHING_ASSISTANT", "GRADING_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TA cannot submit a quiz")
        void taCannotSubmit() throws Exception {
            mockMvc.perform(put("/api/submissions/1/submit").with(csrf())
                            .with(user(userDetails(13L, "ta", "TEACHING_ASSISTANT", "GRADING_MANAGE"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Student cannot access grading endpoints")
    class StudentCannotGrade {

        @Test
        @DisplayName("Student cannot view grading queue")
        void studentCannotViewGradingQueue() throws Exception {
            mockMvc.perform(get("/api/grading/queue")
                            .with(user(userDetails(11L, "stu", "STUDENT", "QUIZ_TAKE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student cannot view grading state")
        void studentCannotViewGradingState() throws Exception {
            mockMvc.perform(get("/api/grading/submissions/1")
                            .with(user(userDetails(11L, "stu", "STUDENT", "QUIZ_TAKE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student cannot grade submissions")
        void studentCannotGradeSubmission() throws Exception {
            GradingRequest request = new GradingRequest();
            request.setSubmissionAnswerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            request.setScore(8.0);
            request.setFeedback("Good");

            mockMvc.perform(post("/api/grading/submissions/1/grade")
                            .with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "QUIZ_TAKE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student cannot add rubric scores")
        void studentCannotAddRubricScores() throws Exception {
            RubricScoreRequest score = new RubricScoreRequest();
            score.setCriterionName("Clarity");
            score.setMaxScore(5.0);
            score.setAwardedScore(4.0);

            mockMvc.perform(post("/api/grading/submissions/1/rubric-scores")
                            .with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "QUIZ_TAKE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(score))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Student can perform authorized submission actions")
    class StudentCanSubmit {

        @Test
        @DisplayName("Student can start a submission")
        void studentCanStartSubmission() throws Exception {
            Submission submission = Submission.builder()
                    .id(1L).attemptNumber(1).status("IN_PROGRESS")
                    .timeRemainingSeconds(3600).build();

            when(submissionService.startSubmission(eq(1L), any())).thenReturn(submission);

            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "QUIZ_TAKE"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Student can submit a quiz")
        void studentCanSubmitQuiz() throws Exception {
            Submission submission = Submission.builder()
                    .id(1L).status("SUBMITTED").build();

            when(submissionService.submitSubmission(eq(1L), any())).thenReturn(submission);

            mockMvc.perform(put("/api/submissions/1/submit").with(csrf())
                            .with(user(userDetails(11L, "stu", "STUDENT", "QUIZ_TAKE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        }
    }

    @Nested
    @DisplayName("Authorized users can access grading")
    class AuthorizedGrading {

        @Test
        @DisplayName("Instructor can grade a submission")
        void instructorCanGrade() throws Exception {
            GradingRequest request = new GradingRequest();
            request.setSubmissionAnswerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            request.setScore(9.0);
            request.setFeedback("Excellent");

            GradingState result = GradingState.builder()
                    .id(1L).status("GRADED").score(BigDecimal.valueOf(9))
                    .feedback("Excellent").build();

            when(gradingWorkflowService.gradeItem(eq(1L), any(), any())).thenReturn(result);

            mockMvc.perform(post("/api/grading/submissions/1/grade")
                            .with(csrf())
                            .with(user(userDetails(20L, "instr", "INSTRUCTOR", "GRADING_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("GRADED"));
        }

        @Test
        @DisplayName("TA can view grading queue")
        void taCanViewQueue() throws Exception {
            SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
            GradingState state = GradingState.builder()
                    .id(1L).submissionAnswer(answer).status("PENDING").build();
            Page<GradingState> page = new PageImpl<>(List.of(state));

            when(gradingWorkflowService.getGradingQueue(any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/grading/queue").param("status", "PENDING")
                            .with(user(userDetails(13L, "ta", "TEACHING_ASSISTANT", "GRADING_VIEW"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
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
                            .with(user(userDetails(13L, "ta", "TEACHING_ASSISTANT", "GRADING_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(score))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("GRADED"));
        }
    }

    @Nested
    @DisplayName("Admin cannot access submission or grading endpoints")
    class AdminCannotAccessSubmissionsOrGrading {

        @Test
        @DisplayName("Admin cannot start submissions")
        void adminCannotStartSubmission() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf())
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin cannot access grading queue")
        void adminCannotAccessGradingQueue() throws Exception {
            mockMvc.perform(get("/api/grading/queue")
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin cannot grade submissions")
        void adminCannotGrade() throws Exception {
            GradingRequest request = new GradingRequest();
            request.setSubmissionAnswerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            request.setScore(8.0);
            request.setFeedback("Good");

            mockMvc.perform(post("/api/grading/submissions/1/grade")
                            .with(csrf())
                            .with(user(userDetails(30L, "adm", "ADMINISTRATOR", "USER_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
