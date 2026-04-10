package com.scholarops.controller;

import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.*;
import com.scholarops.repository.WrongAnswerHistoryRepository;
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
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that unauthenticated requests receive 401 and cross-role requests receive 403
 * across all protected endpoints. The PermissionEvaluator is mocked to always grant
 * permissions so that only role checks cause denial.
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
class AuthorizationDenialTest {

    @Autowired private MockMvc mockMvc;

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

    @BeforeEach
    void grantAllPermissions() {
        when(permissionEvaluator.hasPermission(any(Authentication.class), any(), any()))
                .thenReturn(true);
    }

    // -----------------------------------------------------------------------
    // 401 - Unauthenticated access
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("401 Unauthenticated access")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("GET /api/quizzes returns 401 without authentication")
        void quizListRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/quizzes"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/quizzes/assemble returns 401 without authentication")
        void quizAssembleRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/quizzes/assemble")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"questionBankId\":1,\"title\":\"Q\",\"totalQuestions\":5}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/question-banks returns 401 without authentication")
        void questionBanksRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/question-banks"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/quizzes/1/submissions returns 401 without authentication")
        void submissionStartRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/grading/queue returns 401 without authentication")
        void gradingQueueRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/grading/queue"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/schedules/1/move returns 401 without authentication")
        void timetableMoveRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/schedules/1/move")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStartTime\":\"2024-06-01T09:00\",\"newEndTime\":\"2024-06-01T10:00\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/catalog returns 401 without authentication")
        void catalogRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/content returns 401 without authentication")
        void contentListRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/content"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/crawl-sources returns 401 without authentication")
        void crawlSourcesRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/crawl-sources"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/users returns 401 without authentication")
        void usersListRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/roles returns 401 without authentication")
        void rolesRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/roles"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/audit-logs returns 401 without authentication")
        void auditLogsRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/plagiarism/checks returns 401 without authentication")
        void plagiarismChecksRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/plagiarism/checks"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/wrong-answers returns 401 without authentication")
        void wrongAnswersRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/wrong-answers"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/schedules returns 401 without authentication")
        void schedulesRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/schedules"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -----------------------------------------------------------------------
    // 403 - Cross-role access: STUDENT accessing INSTRUCTOR-only endpoints
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("403 Student cannot access instructor endpoints")
    class StudentCannotAccessInstructorEndpoints {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot assemble quizzes")
        void studentCannotAssembleQuiz() throws Exception {
            mockMvc.perform(post("/api/quizzes/assemble")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"questionBankId\":1,\"title\":\"Q\",\"totalQuestions\":5}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot list quizzes as instructor")
        void studentCannotListQuizzesAsInstructor() throws Exception {
            mockMvc.perform(get("/api/quizzes"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot publish quizzes")
        void studentCannotPublishQuiz() throws Exception {
            mockMvc.perform(put("/api/quizzes/1/publish").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot access question banks")
        void studentCannotAccessQuestionBanks() throws Exception {
            mockMvc.perform(get("/api/question-banks"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot create question banks")
        void studentCannotCreateQuestionBank() throws Exception {
            mockMvc.perform(post("/api/question-banks")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Bank\",\"description\":\"desc\",\"subject\":\"Math\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot access grading queue")
        void studentCannotAccessGradingQueue() throws Exception {
            mockMvc.perform(get("/api/grading/queue"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot grade submissions")
        void studentCannotGrade() throws Exception {
            mockMvc.perform(post("/api/grading/submissions/1/grade")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"score\":8.0,\"feedback\":\"Good\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot access plagiarism checks")
        void studentCannotAccessPlagiarism() throws Exception {
            mockMvc.perform(get("/api/plagiarism/checks"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // 403 - Cross-role access: STUDENT accessing ADMIN-only endpoints
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("403 Student cannot access admin endpoints")
    class StudentCannotAccessAdminEndpoints {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot list users")
        void studentCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot create users")
        void studentCannotCreateUser() throws Exception {
            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"new\",\"email\":\"a@b.com\",\"password\":\"pass\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot list roles")
        void studentCannotListRoles() throws Exception {
            mockMvc.perform(get("/api/roles"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot assign roles")
        void studentCannotAssignRoles() throws Exception {
            mockMvc.perform(post("/api/users/1/roles")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"roleId\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot view audit logs")
        void studentCannotViewAuditLogs() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot view permission change history")
        void studentCannotViewPermissionHistory() throws Exception {
            mockMvc.perform(get("/api/permission-change-history"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // 403 - Cross-role access: INSTRUCTOR accessing ADMIN-only endpoints
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("403 Instructor cannot access admin endpoints")
    class InstructorCannotAccessAdminEndpoints {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot list users")
        void instructorCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot create users")
        void instructorCannotCreateUser() throws Exception {
            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"new\",\"email\":\"a@b.com\",\"password\":\"pass\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot assign roles")
        void instructorCannotAssignRoles() throws Exception {
            mockMvc.perform(post("/api/users/1/roles")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"roleId\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot view audit logs")
        void instructorCannotViewAuditLogs() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // 403 - Cross-role access: ADMIN accessing student-only endpoints
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("403 Admin cannot access student-only endpoints")
    class AdminCannotAccessStudentEndpoints {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot start submissions")
        void adminCannotStartSubmission() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot autosave submissions")
        void adminCannotAutosave() throws Exception {
            mockMvc.perform(put("/api/submissions/1/autosave")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"timeRemainingSeconds\":1800,\"answers\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot manage schedules")
        void adminCannotManageSchedules() throws Exception {
            mockMvc.perform(get("/api/schedules"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot view wrong answers")
        void adminCannotViewWrongAnswers() throws Exception {
            mockMvc.perform(get("/api/wrong-answers"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot move timetable entries")
        void adminCannotMoveTimetable() throws Exception {
            mockMvc.perform(post("/api/schedules/1/move")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStartTime\":\"2024-06-01T09:00\",\"newEndTime\":\"2024-06-01T10:00\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // 403 - Cross-role access: INSTRUCTOR accessing CONTENT_CURATOR endpoints
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("403 Instructor cannot access content curator endpoints")
    class InstructorCannotAccessCuratorEndpoints {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot list content for review")
        void instructorCannotListContent() throws Exception {
            mockMvc.perform(get("/api/content"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot publish content")
        void instructorCannotPublishContent() throws Exception {
            mockMvc.perform(post("/api/content/1/publish").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot manage crawl sources")
        void instructorCannotManageCrawlSources() throws Exception {
            mockMvc.perform(get("/api/crawl-sources"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // 403 - Cross-role access: INSTRUCTOR accessing STUDENT-only endpoints
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("403 Instructor cannot access student-only endpoints")
    class InstructorCannotAccessStudentEndpoints {

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot start quiz submissions")
        void instructorCannotStartSubmission() throws Exception {
            mockMvc.perform(post("/api/quizzes/1/submissions").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot submit quiz answers")
        void instructorCannotSubmitQuiz() throws Exception {
            mockMvc.perform(put("/api/submissions/1/submit").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot manage own schedule")
        void instructorCannotManageSchedule() throws Exception {
            mockMvc.perform(get("/api/schedules"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot view wrong answers")
        void instructorCannotViewWrongAnswers() throws Exception {
            mockMvc.perform(get("/api/wrong-answers"))
                    .andExpect(status().isForbidden());
        }
    }
}
