package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.GradingRequest;
import com.scholarops.model.dto.RubricScoreRequest;
import com.scholarops.model.entity.GradingState;
import com.scholarops.model.entity.Submission;
import com.scholarops.model.entity.SubmissionAnswer;
import com.scholarops.repository.WrongAnswerHistoryRepository;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.*;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests that having the correct role but lacking the required specific permission
 * results in 403 Forbidden. The PermissionEvaluator mock is configured to deny
 * all permissions unless explicitly allowed per test.
 */
@WebMvcTest(
        value = {
                QuizController.class,
                QuestionBankController.class,
                SubmissionController.class,
                GradingController.class,
                TimetableController.class,
                ScheduleController.class,
                CatalogController.class,
                ContentController.class,
                CrawlSourceController.class,
                UserController.class,
                RoleController.class,
                AuditLogController.class,
                PlagiarismController.class,
                WrongAnswerController.class
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class PermissionGranularityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private QuizAssemblyService quizAssemblyService;
    @MockBean private QuestionBankService questionBankService;
    @MockBean private SubmissionService submissionService;
    @MockBean private GradingWorkflowService gradingWorkflowService;
    @MockBean private TimetableService timetableService;
    @MockBean private ScheduleService scheduleService;
    @MockBean private CatalogService catalogService;
    @MockBean private ContentStandardizationService contentStandardizationService;
    @MockBean private CrawlSourceService crawlSourceService;
    @MockBean private UserService userService;
    @MockBean private RoleService roleService;
    @MockBean private AuditLogService auditLogService;
    @MockBean private PlagiarismService plagiarismService;
    @MockBean private WrongAnswerHistoryRepository wrongAnswerHistoryRepository;

    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private PermissionEvaluator permissionEvaluator;

    // -----------------------------------------------------------------------
    // INSTRUCTOR role without QUIZ_MANAGE permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Instructor without QUIZ_MANAGE permission")
    class InstructorWithoutQuizManage {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Cannot assemble quiz without QUIZ_MANAGE")
        void cannotAssembleWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/quizzes/assemble")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"questionBankId\":1,\"title\":\"Q\",\"totalQuestions\":5}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Cannot list quizzes without QUIZ_MANAGE")
        void cannotListQuizzesWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/quizzes"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Cannot publish quiz without QUIZ_MANAGE")
        void cannotPublishWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(put("/api/quizzes/1/publish").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Cannot schedule quiz without QUIZ_MANAGE")
        void cannotScheduleWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(put("/api/quizzes/1/schedule")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"releaseStart\":\"2024-06-01T08:00\",\"releaseEnd\":\"2024-06-01T17:00\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // INSTRUCTOR role without QUESTION_BANK_MANAGE permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Instructor without QUESTION_BANK_MANAGE permission")
    class InstructorWithoutQuestionBankManage {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Cannot list question banks without QUESTION_BANK_MANAGE")
        void cannotListBanksWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUESTION_BANK_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/question-banks"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Cannot create question bank without QUESTION_BANK_MANAGE")
        void cannotCreateBankWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUESTION_BANK_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/question-banks")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Bank\",\"description\":\"d\",\"subject\":\"Math\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Cannot add questions without QUESTION_BANK_MANAGE")
        void cannotAddQuestionWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUESTION_BANK_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/question-banks/1/questions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"questionText\":\"What is 2+2?\",\"questionType\":\"MULTIPLE_CHOICE\",\"difficultyLevel\":\"EASY\",\"points\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Cannot list knowledge tags without QUESTION_BANK_MANAGE")
        void cannotListTagsWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUESTION_BANK_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/knowledge-tags"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // STUDENT role without QUIZ_TAKE permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Student without QUIZ_TAKE permission")
    class StudentWithoutQuizTake {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot start submission without QUIZ_TAKE")
        void cannotStartSubmissionWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_TAKE")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot autosave without QUIZ_TAKE")
        void cannotAutosaveWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_TAKE")))
                    .thenReturn(false);

            mockMvc.perform(put("/api/submissions/1/autosave")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"timeRemainingSeconds\":1800,\"answers\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot submit without QUIZ_TAKE")
        void cannotSubmitWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_TAKE")))
                    .thenReturn(false);

            mockMvc.perform(put("/api/submissions/1/submit").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // STUDENT role without SUBMISSION_VIEW_OWN permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Student without SUBMISSION_VIEW_OWN permission")
    class StudentWithoutSubmissionViewOwn {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot view own submission without SUBMISSION_VIEW_OWN")
        void cannotViewSubmissionWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("SUBMISSION_VIEW_OWN")))
                    .thenReturn(false);
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("SUBMISSION_VIEW_ALL")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/submissions/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot view feedback without SUBMISSION_VIEW_OWN")
        void cannotViewFeedbackWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("SUBMISSION_VIEW_OWN")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/submissions/1/feedback"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // STUDENT without SCHEDULE_MANAGE_OWN permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Student without SCHEDULE_MANAGE_OWN permission")
    class StudentWithoutScheduleManageOwn {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot list schedules without SCHEDULE_MANAGE_OWN")
        void cannotListSchedulesWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("SCHEDULE_MANAGE_OWN")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/schedules"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot move schedule without SCHEDULE_MANAGE_OWN")
        void cannotMoveWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("SCHEDULE_MANAGE_OWN")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/schedules/1/move")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStartTime\":\"2024-06-01T09:00\",\"newEndTime\":\"2024-06-01T10:00\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot undo without SCHEDULE_MANAGE_OWN")
        void cannotUndoWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("SCHEDULE_MANAGE_OWN")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/schedules/undo").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // STUDENT without WRONG_ANSWER_VIEW_OWN permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Student without WRONG_ANSWER_VIEW_OWN permission")
    class StudentWithoutWrongAnswerViewOwn {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot view wrong answers without WRONG_ANSWER_VIEW_OWN")
        void cannotViewWrongAnswersWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("WRONG_ANSWER_VIEW_OWN")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/wrong-answers"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Cannot view wrong answers for question without WRONG_ANSWER_VIEW_OWN")
        void cannotViewWrongAnswersForQuestionWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("WRONG_ANSWER_VIEW_OWN")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/wrong-answers/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // TEACHING_ASSISTANT / INSTRUCTOR without GRADING_MANAGE permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Grading roles without GRADING_MANAGE permission")
    class GradingRolesWithoutGradingManage {

        @Test
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA cannot grade without GRADING_MANAGE")
        void taCannotGradeWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("GRADING_MANAGE")))
                    .thenReturn(false);

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
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot grade without GRADING_MANAGE")
        void instructorCannotGradeWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("GRADING_MANAGE")))
                    .thenReturn(false);

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
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA cannot add rubric scores without GRADING_MANAGE")
        void taCannotAddRubricWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("GRADING_MANAGE")))
                    .thenReturn(false);

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
    // TEACHING_ASSISTANT / INSTRUCTOR without GRADING_VIEW permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Grading roles without GRADING_VIEW permission")
    class GradingRolesWithoutGradingView {

        @Test
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA cannot view grading queue without GRADING_VIEW")
        void taCannotViewQueueWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("GRADING_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/grading/queue"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot view grading state without GRADING_VIEW")
        void instructorCannotViewStateWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("GRADING_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/grading/submissions/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // ADMINISTRATOR without AUDIT_VIEW permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Admin without AUDIT_VIEW permission")
    class AdminWithoutAuditView {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot view audit logs without AUDIT_VIEW")
        void cannotViewAuditLogsWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("AUDIT_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot view permission history without AUDIT_VIEW")
        void cannotViewPermissionHistoryWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("AUDIT_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/permission-change-history"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // ADMINISTRATOR without USER_MANAGE permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Admin without USER_MANAGE permission")
    class AdminWithoutUserManage {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot list users without USER_MANAGE")
        void cannotListUsersWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("USER_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot create users without USER_MANAGE")
        void cannotCreateUserWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("USER_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"new\",\"email\":\"a@b.com\",\"password\":\"pass\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot delete users without USER_MANAGE")
        void cannotDeleteUserWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("USER_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(delete("/api/users/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // ADMINISTRATOR without ROLE_ASSIGN permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Admin without ROLE_ASSIGN permission")
    class AdminWithoutRoleAssign {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot list roles without ROLE_ASSIGN")
        void cannotListRolesWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("ROLE_ASSIGN")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/roles"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot assign roles without ROLE_ASSIGN")
        void cannotAssignRoleWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("ROLE_ASSIGN")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/users/1/roles")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"roleId\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot revoke roles without ROLE_ASSIGN")
        void cannotRevokeRoleWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("ROLE_ASSIGN")))
                    .thenReturn(false);

            mockMvc.perform(delete("/api/users/1/roles/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // ADMINISTRATOR without PASSWORD_ADMIN_RESET permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Admin without PASSWORD_ADMIN_RESET permission")
    class AdminWithoutPasswordAdminReset {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Cannot admin-reset password without PASSWORD_ADMIN_RESET")
        void cannotAdminResetWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("PASSWORD_ADMIN_RESET")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/users/1/admin-reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"newpass123\",\"workstationId\":\"WS-001\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // CONTENT_CURATOR without CONTENT_REVIEW permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Content curator without CONTENT_REVIEW permission")
    class CuratorWithoutContentReview {

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Cannot list content without CONTENT_REVIEW")
        void cannotListContentWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_REVIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/content"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Cannot publish content without CONTENT_REVIEW")
        void cannotPublishWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_REVIEW")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/content/1/publish").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // CONTENT_CURATOR without CRAWL_SOURCE_MANAGE permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Content curator without CRAWL_SOURCE_MANAGE permission")
    class CuratorWithoutCrawlSourceManage {

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Cannot list crawl sources without CRAWL_SOURCE_MANAGE")
        void cannotListCrawlSourcesWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CRAWL_SOURCE_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/crawl-sources"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Cannot create crawl sources without CRAWL_SOURCE_MANAGE")
        void cannotCreateCrawlSourceWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CRAWL_SOURCE_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(post("/api/crawl-sources")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\":\"https://example.com\",\"name\":\"Test\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Cannot delete crawl sources without CRAWL_SOURCE_MANAGE")
        void cannotDeleteCrawlSourceWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CRAWL_SOURCE_MANAGE")))
                    .thenReturn(false);

            mockMvc.perform(delete("/api/crawl-sources/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // INSTRUCTOR / TA without PLAGIARISM_VIEW permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Instructor/TA without PLAGIARISM_VIEW permission")
    class WithoutPlagiarismView {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot list plagiarism checks without PLAGIARISM_VIEW")
        void instructorCannotListChecksWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("PLAGIARISM_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/plagiarism/checks"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA cannot view plagiarism check without PLAGIARISM_VIEW")
        void taCannotViewCheckWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("PLAGIARISM_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/plagiarism/checks/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot view plagiarism matches without PLAGIARISM_VIEW")
        void instructorCannotViewMatchesWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("PLAGIARISM_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/plagiarism/checks/1/matches"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // Positive: correct role + permission grants access
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Correct role with correct permission grants access")
    class CorrectRoleAndPermissionGrantsAccess {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor with QUIZ_MANAGE can list quizzes")
        void instructorWithQuizManageCanList() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_MANAGE")))
                    .thenReturn(true);
            when(quizAssemblyService.listQuizzes(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/quizzes"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA with GRADING_VIEW can view grading queue")
        void taWithGradingViewCanViewQueue() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("GRADING_VIEW")))
                    .thenReturn(true);
            Page<GradingState> page = new PageImpl<>(List.of());
            when(gradingWorkflowService.getGradingQueue(any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/grading/queue").param("status", "PENDING"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student with QUIZ_TAKE can start submission")
        void studentWithQuizTakeCanStart() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("QUIZ_TAKE")))
                    .thenReturn(true);

            Submission submission = Submission.builder()
                    .id(1L).attemptNumber(1).status("IN_PROGRESS")
                    .timeRemainingSeconds(3600).build();
            when(submissionService.startSubmission(eq(1L), any())).thenReturn(submission);

            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin with AUDIT_VIEW can view audit logs")
        void adminWithAuditViewCanViewLogs() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("AUDIT_VIEW")))
                    .thenReturn(true);
            when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isOk());
        }
    }
}
