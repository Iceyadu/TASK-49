# ScholarOps System Design Document

**System:** ScholarOps Offline Learning & Content Intake System
**Version:** 1.0
**Last Updated:** 2026-04-09

---

## 1. System Overview

ScholarOps is an offline-runnable learning management and content intake system designed for institutional environments where reliable internet connectivity cannot be assumed. The system supports the full lifecycle of educational content -- from web crawling and standardization through quiz assembly, assessment delivery, grading, and student review.

### Core Design Principles

- **Offline-first operation.** Every core workflow (authentication, content browsing, quiz taking, grading, timetable management) runs entirely on localhost or LAN with zero external service dependencies.
- **Role-based access control.** Five distinct roles govern access at the menu, API, and data-object levels.
- **Auditability.** All mutating operations are recorded in `audit_logs` with user ID, action type, entity reference, IP address, and workstation identifier.
- **Data integrity.** Flyway-managed migrations, foreign key constraints, and transactional service methods ensure consistent state.

### Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | Vue.js 3 (Composition API) + TypeScript | 3.x |
| Build Tool | Vite | latest |
| State Management | Pinia (`@/stores/auth`) | latest |
| HTTP Client | Axios (`@/api/client.ts`) | latest |
| Backend | Spring Boot | 3.2 |
| ORM | Hibernate / Spring Data JPA | 6.x |
| Database | MySQL | 8.0 |
| Migrations | Flyway | latest |
| Auth Tokens | JJWT (io.jsonwebtoken) | latest |
| Reverse Proxy | nginx | latest |
| Containerization | Docker Compose | 3.8 |

---

## 2. Architecture

ScholarOps follows a strict 3-tier architecture with clear separation between presentation, application logic, and persistence.

### Text Diagram

```
+-----------------------------------------------------------+
|                      Client Browser                        |
|  Vue.js 3 SPA (Composition API + Pinia + Vue Router)      |
|  Composables: useAuth, useAutosave, useCountdown,         |
|               useDragDrop, useUndoRedo, usePermission      |
+-----------------------------------------------------------+
                          |
                     HTTP / HTTPS
                          |
+-----------------------------------------------------------+
|                    nginx (port 80)                         |
|  - Serves built Vue.js static assets                      |
|  - Proxies /api/* to backend:8080                         |
|  - Security headers (X-Frame-Options, X-Content-Type-     |
|    Options, X-XSS-Protection, Referrer-Policy)            |
|  - Gzip compression for text/css/json/js                  |
|  - Static asset caching (1y, immutable)                   |
+-----------------------------------------------------------+
                          |
                  /api/ proxy_pass
                          |
+-----------------------------------------------------------+
|              Spring Boot 3.2 (port 8080)                  |
|                                                           |
|  SecurityFilterChain                                      |
|    +-> JwtAuthenticationFilter                            |
|    +-> @PreAuthorize / PermissionEvaluatorImpl             |
|                                                           |
|  Controllers (17 REST controllers)                        |
|    +-> Services (20 service classes)                      |
|       +-> Repositories (Spring Data JPA)                  |
|                                                           |
|  Async Processing:                                        |
|    crawlExecutor (core=2, max=5, queue=25)                |
|    gradingExecutor (core=2, max=4, queue=50)              |
+-----------------------------------------------------------+
                          |
                     JDBC (HikariCP)
                     pool: 5-20
                          |
+-----------------------------------------------------------+
|                MySQL 8.0 (port 3306)                      |
|  Database: scholarops                                     |
|  Charset: utf8mb4_unicode_ci                              |
|  Engine: InnoDB (all tables)                              |
|  28+ tables across 8 domain groups                        |
+-----------------------------------------------------------+
```

### Request Flow

1. The browser loads the Vue.js SPA from nginx (static HTML/JS/CSS).
2. The SPA makes API calls to `/api/*` endpoints.
3. nginx proxies these requests to `http://backend:8080/api/`.
4. The `JwtAuthenticationFilter` (`security/JwtAuthenticationFilter.java`) extracts the JWT from the `Authorization` header, validates it via `JwtTokenProvider`, and populates the Spring `SecurityContext`.
5. Controller methods protected by `@PreAuthorize` delegate to `PermissionEvaluatorImpl` for fine-grained permission checks.
6. Services execute business logic within `@Transactional` boundaries.
7. Repositories interact with MySQL via Spring Data JPA.
8. Responses flow back through the same path as `ApiResponse<T>` or `PagedResponse<T>` DTOs.

### Statelessness

The backend uses `SessionCreationPolicy.STATELESS` (configured in `config/SecurityConfig.java`). There are no server-side HTTP sessions. All authentication state is carried in JWT access and refresh tokens.

---

## 3. Role Model

The system defines five roles in `model/enums/RoleName.java`, seeded by `V1__init_schema.sql`. Each role is mapped to a set of permission codes via the `role_permissions` join table.

### 3.1 Administrator (`ADMINISTRATOR`)

**All 19 permissions.** Full unrestricted access to every subsystem.

| Capability | Permission Code | Code Path |
|---|---|---|
| Create, update, soft-delete users | `USER_MANAGE` | `UserController` -> `UserService.createUser()`, `updateUser()`, `deleteUser()` |
| Assign and revoke roles | `ROLE_ASSIGN` | `RoleController` -> `RoleService` |
| View audit logs and permission change history | `AUDIT_VIEW` | `AuditLogController` -> `AuditLogService` |
| Admin-assisted password reset (bypasses old password) | `PASSWORD_ADMIN_RESET` | `UserController` -> `UserService.adminResetPassword()` |
| All crawl, content, assessment, grading, and schedule permissions | (all) | Inherited via full permission grant |

### 3.2 Content Curator (`CONTENT_CURATOR`)

Manages the crawl-and-ingest pipeline.

| Permission Code | Capability |
|---|---|
| `CRAWL_SOURCE_MANAGE` | Create, update, enable/disable crawl source profiles |
| `CRAWL_RULE_MANAGE` | Create versioned extraction rules (XPath, CSS Selector, Regex), activate/revert rule versions |
| `CRAWL_RUN_MANAGE` | Start, monitor, and cancel crawl runs |
| `CONTENT_REVIEW` | Review raw content, publish standardized content records |
| `CONTENT_VIEW` | Browse the content catalog |

### 3.3 Instructor (`INSTRUCTOR`)

Manages assessment lifecycle.

| Permission Code | Capability |
|---|---|
| `QUESTION_BANK_MANAGE` | Create question banks, add/edit/delete questions with knowledge tags and difficulty levels |
| `QUIZ_MANAGE` | Assemble quizzes from question banks using rules, set release windows, publish |
| `SUBMISSION_VIEW_ALL` | View all student submissions for assigned quizzes |
| `GRADING_MANAGE` | Grade subjective items, add rubric scores |
| `GRADING_VIEW` | View grading queue and states |
| `CONTENT_VIEW` | Browse published content for quiz reference |
| `PLAGIARISM_VIEW` | View plagiarism check results and flagged submissions |

### 3.4 Teaching Assistant (`TEACHING_ASSISTANT`)

Focused on grading support.

| Permission Code | Capability |
|---|---|
| `GRADING_MANAGE` | Grade subjective items from the grading queue, apply rubric scores |
| `GRADING_VIEW` | View grading states and queue |
| `SUBMISSION_VIEW_ALL` | View submissions for context during grading |
| `CONTENT_VIEW` | Browse content catalog |
| `PLAGIARISM_VIEW` | View plagiarism results for submissions under review |

### 3.5 Student (`STUDENT`)

Consumer-facing role.

| Permission Code | Capability |
|---|---|
| `CONTENT_VIEW` | Browse published content catalog with full-text search |
| `QUIZ_TAKE` | Take published quizzes within release windows, with countdown timer and autosave |
| `SUBMISSION_VIEW_OWN` | View own submission history and scores |
| `SCHEDULE_MANAGE_OWN` | Create, edit, drag-and-drop timetable entries; undo/redo; locked period enforcement |
| `WRONG_ANSWER_VIEW_OWN` | Review wrong answer history with explanations |

### Route-Level Enforcement

Frontend route guards (`router/index.ts`, `guards/authGuard.ts`) enforce role requirements via the `meta.roles` array on each route definition. For example:

- `/admin/*` routes require `ADMINISTRATOR`
- `/curator/*` routes require `CONTENT_CURATOR`
- `/instructor/*` routes require `INSTRUCTOR`
- `/ta/*` routes require `TEACHING_ASSISTANT` or `INSTRUCTOR`
- `/student/*` routes require `STUDENT`

---

## 4. Security Model

### 4.1 Authentication

**JWT Dual-Token Strategy** (`security/JwtTokenProvider.java`):

- **Access Token:** Contains `sub` (user ID), `username`, `roles` claims. Signed with HMAC-SHA using a Base64-decoded secret key. Expires in **1 hour** (`scholarops.jwt.expiration-ms: 3600000`).
- **Refresh Token:** Contains `sub` (user ID) and `type: "refresh"` claim. Expires in **24 hours** (`scholarops.jwt.refresh-expiration-ms: 86400000`).
- Public endpoints: only `/api/auth/login` and `/api/auth/refresh` are unauthenticated.
- All other `/api/**` endpoints require a valid JWT.
- Any request not matching `/api/**` is denied (`anyRequest().denyAll()`).

**Password Hashing:** BCrypt with cost factor 12 (`SecurityConfig.passwordEncoder()`).

### 4.2 Password Policy

Enforced by `security/PasswordPolicyValidator.java`:

| Rule | Implementation |
|---|---|
| Minimum 12 characters | `MIN_LENGTH = 12` |
| At least one uppercase letter | `Pattern.compile("[A-Z]")` |
| At least one lowercase letter | `Pattern.compile("[a-z]")` |
| At least one digit | `Pattern.compile("[0-9]")` |
| At least one symbol | `Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?` `` `~]")` |

Violations are collected into a list and thrown as `PasswordPolicyViolationException`, which the `GlobalExceptionHandler` renders as a 400 response with structured violation details.

### 4.3 Admin-Assisted Password Reset

Implemented in `UserService.adminResetPassword()`:

1. Requires the `PASSWORD_ADMIN_RESET` permission.
2. Requires a `workstationId` in the request body (rejects blank values with `IllegalArgumentException`).
3. Validates the new password against `PasswordPolicyValidator`.
4. Logs the reset with full details: admin ID, target username, workstation ID, timestamp, and reason.
5. Audit entry uses `AuditAction.USER_ADMIN_PASSWORD_RESET` with the workstation ID stored in the `workstation_id` column.

Self-service password reset (`UserService.resetPassword()`) requires the old password to be verified first.

### 4.4 Authorization Layers

**Layer 1 -- Menu-Level (Frontend):**
- Vue Router `beforeEach` guard checks `authStore.isAuthenticated` and `authStore.hasRole()`.
- `authGuard.ts` additionally checks `meta.permissions` for fine-grained permission gates.
- Unauthenticated users are redirected to `/login`; unauthorized users to `/forbidden`.

**Layer 2 -- API-Level (Backend):**
- `@EnableMethodSecurity(prePostEnabled = true)` on `SecurityConfig`.
- Controllers use `@PreAuthorize("hasAuthority('PERMISSION_CODE')")` annotations.
- `PermissionEvaluatorImpl` provides custom `hasPermission()` evaluation.

**Layer 3 -- Object-Level (Services):**
- Ownership checks in service methods (e.g., `ScheduleService.getOwnSchedule()` verifies `schedule.getUser().getId().equals(userId)`).
- `QuizAssemblyService.publishQuiz()` checks `quiz.getCreatedBy().getId().equals(userId)`.
- `TimetableService.deleteLockedPeriod()` verifies period ownership before deletion.

### 4.5 Encryption at Rest

Crawl source credentials are encrypted using AES-256-CBC (`util/AesEncryptionUtil.java`):

- **Algorithm:** `AES/CBC/PKCS5Padding`
- **Key Length:** 256-bit (32-byte key, zero-padded if shorter)
- **IV:** 16-byte random IV generated per encryption via `SecureRandom`
- **Storage:** Encrypted fields stored as `VARBINARY(512)` in `encrypted_source_credentials`; the IV stored as `VARBINARY(16)` in the `encryption_iv` column.
- **Key Source:** Environment variable `SCHOLAROPS_AES_KEY` (configured via `config/EncryptionConfig.java`).

### 4.6 Additional Security Measures

| Measure | Implementation |
|---|---|
| CORS restrictions | `CorsConfig.java` -- allowed origins limited to `localhost:5173`, `localhost:3000`, `localhost:80` |
| Allowed headers | `Authorization`, `Content-Type`, `X-Workstation-Id` |
| CSRF disabled | Stateless JWT architecture makes CSRF tokens unnecessary |
| Security headers (nginx) | `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, `X-XSS-Protection: 1; mode=block`, `Referrer-Policy: strict-origin-when-cross-origin` |
| No public debug endpoints | No actuator, Swagger, or debug endpoints exposed; `anyRequest().denyAll()` for non-API paths |
| Sensitive data masking | `GlobalExceptionHandler` returns generic messages for unexpected errors; password policy violations list rules, not the password itself |
| Rate limiting | `RateLimitExceededException` handled with `429 Too Many Requests` and `Retry-After` header |

---

## 5. Data Model Overview

The database consists of **28 tables** across 8 domain groups, all using InnoDB engine with `utf8mb4_unicode_ci` collation. Schema is managed by Flyway migration `V1__init_schema.sql`.

### 5.1 Auth & RBAC (7 tables)

```
users                          roles
  PK: id (BIGINT)                PK: id (BIGINT)
  username (UNIQUE)              name (UNIQUE)
  email (UNIQUE)                 description
  password_hash
  enabled, account_locked            permissions
  last_login_at                        PK: id (BIGINT)
  Indexes: email, username,            code (UNIQUE)
           enabled                     category
                                       Index: category
        user_roles
          PK: id                   role_permissions
          FK: user_id -> users       PK: id
          FK: role_id -> roles       FK: role_id -> roles
          FK: assigned_by -> users   FK: permission_id -> permissions
          UNIQUE: (user_id, role_id) UNIQUE: (role_id, permission_id)

audit_logs                         permission_change_history
  PK: id                            PK: id
  FK: user_id -> users              FK: target_user_id -> users
  action, entity_type, entity_id    FK: changed_by_user_id -> users
  details, ip_address,              FK: role_id -> roles
  workstation_id                    FK: permission_id -> permissions
  Indexes: action, entity,          change_type, old_value, new_value
           user, created_at         reason
                                    Indexes: target, changed_by, created_at
```

**Key relationships:** `users` --(M:N via `user_roles`)--> `roles` --(M:N via `role_permissions`)--> `permissions`. The `assigned_by` FK on `user_roles` traces who granted each role.

### 5.2 Crawling (4 tables)

```
crawl_source_profiles              encrypted_source_credentials
  PK: id                            PK: id
  name, base_url                     FK: source_profile_id (UNIQUE)
  rate_limit_per_minute                  -> crawl_source_profiles
  requires_auth, enabled             encrypted_username (VARBINARY)
  FK: created_by -> users            encrypted_password (VARBINARY)
  Indexes: enabled, creator          encrypted_api_key (VARBINARY)
                                     encryption_iv (VARBINARY(16))

crawl_rule_versions                crawl_runs
  PK: id                            PK: id
  FK: source_profile_id             FK: source_profile_id
  version_number                    FK: rule_version_id
  extraction_method (ENUM)          FK: initiated_by -> users
  rule_definition (JSON)            status (PENDING/RUNNING/COMPLETED/
  field_mappings (JSON)                     FAILED/CANCELLED)
  type_validations (JSON)           started_at, completed_at
  is_active                         total_pages, pages_crawled,
  UNIQUE: (source_profile_id,         pages_failed, items_extracted
           version_number)          error_log
  Indexes: source, active           Indexes: status, source, created_at
```

**Key relationships:** Each `crawl_source_profile` has at most one set of `encrypted_source_credentials` (1:1). Multiple `crawl_rule_versions` per source, but only one `is_active` at a time. Each `crawl_run` references a specific source profile and rule version.

### 5.3 Content (2 tables)

```
standardized_content_records       media_metadata
  PK: id                            PK: id
  FK: crawl_run_id -> crawl_runs    FK: content_record_id
  FK: source_profile_id                  -> standardized_content_records
  FK: published_by -> users          media_type, file_name, file_size
  title, description, body_text      mime_type, local_path
  content_type, source_url           width, height, duration_seconds
  original_timestamp,                checksum
  standardized_timestamp             Indexes: content, media_type
  timezone_id
  original_location,
  normalized_address
  detected_language, price
  is_published, published_at
  FULLTEXT INDEX: (title,
    description, body_text)
  Indexes: published, type,
    availability, popularity,
    created_at
```

**Key indexes:** The `FULLTEXT INDEX ft_content_search` on `title`, `description`, and `body_text` enables catalog full-text search via `CatalogService`.

### 5.4 Scheduling (3 tables)

```
schedules                          schedule_change_journal
  PK: id                            PK: id
  FK: user_id -> users              FK: user_id -> users
  title, description                FK: schedule_id -> schedules
  start_time, end_time              change_type (CREATE/UPDATE/
  day_of_week, is_recurring           DELETE/MOVE/MERGE/SPLIT)
  color                             previous_state (JSON)
  content_record_id                 new_state (JSON)
  quiz_paper_id                     is_undone, sequence_number
  Indexes: user, time,              Indexes: user, (user, sequence)
           (user, day_of_week)

locked_periods
  PK: id
  FK: user_id -> users
  title, start_time, end_time
  day_of_week, reason
  Indexes: user, time
```

**Key relationships:** The `schedule_change_journal` provides full undo/redo support by storing `previous_state` and `new_state` as JSON snapshots. `locked_periods` define non-movable time blocks that prevent schedule conflicts.

### 5.5 Assessment (7 tables)

```
question_banks                     knowledge_tags
  PK: id                            PK: id
  name, description, subject         name (UNIQUE)
  FK: created_by -> users            category

questions                          question_knowledge_tags
  PK: id                            PK: (question_id, tag_id)
  FK: question_bank_id              FK: question_id -> questions
  question_type (SINGLE_CHOICE/     FK: tag_id -> knowledge_tags
    MULTIPLE_CHOICE/TRUE_FALSE/
    SHORT_ANSWER/ESSAY)
  difficulty_level (1-5)
  question_text, options (JSON)
  correct_answer, explanation
  points
  Indexes: bank, difficulty, type

quiz_papers                        quiz_questions
  PK: id                            PK: (quiz_paper_id, question_id)
  FK: question_bank_id              question_order
  FK: created_by -> users
  title, total_questions,
  total_points
  time_limit_minutes, max_attempts
  release_start, release_end
  is_published, shuffle_questions
  show_immediate_feedback
  Indexes: bank, published, release

quiz_rules
  PK: id
  FK: quiz_paper_id
  FK: tag_id -> knowledge_tags
  rule_type (DIFFICULTY/TAG/...)
  min_count, max_count
  difficulty_level
```

### 5.6 Submissions (2 tables)

```
submissions                        submission_answers
  PK: id                            PK: id
  FK: quiz_paper_id                  FK: submission_id -> submissions
  FK: student_id -> users            FK: question_id -> questions
  attempt_number                     answer_text, selected_option
  status (IN_PROGRESS/SUBMITTED/     is_correct, score
    AUTO_GRADED/FULLY_GRADED)        auto_graded, graded_at
  started_at, submitted_at           UNIQUE: (submission_id,
  auto_saved_at                              question_id)
  time_remaining_seconds
  total_score, max_score,
  percentage, fingerprint_hash
  UNIQUE: (quiz_paper_id,
    student_id, attempt_number)
  Indexes: student, quiz, status
```

### 5.7 Grading (2 tables)

```
grading_states                     rubric_scores
  PK: id                            PK: id
  FK: submission_answer_id           FK: grading_state_id
  status (PENDING/ASSIGNED/              -> grading_states
    IN_REVIEW/GRADED/RETURNED)       criterion_name
  FK: assigned_to -> users           max_score, awarded_score
  FK: graded_by -> users             comment
  score, feedback                    Index: grading_state_id
  graded_at
  Indexes: status, assigned,
           answer
```

### 5.8 Review & Plagiarism (3 tables)

```
wrong_answer_history               plagiarism_checks
  PK: id                            PK: id
  FK: student_id -> users            FK: submission_id -> submissions
  FK: question_id -> questions       status, max_similarity_score
  FK: submission_id -> submissions   is_flagged, checked_at
  student_answer, correct_answer     Indexes: submission, flagged
  explanation
  reviewed, reviewed_at            plagiarism_matches
  Indexes: student, question,        PK: id
           (student, reviewed)       FK: plagiarism_check_id
                                     FK: matched_submission_id
                                     FK: matched_content_id
                                     similarity_score
                                     matched_text_excerpt
                                     source_text_excerpt
                                     Indexes: check, score
```

---

## 6. Module Decomposition

### 6.1 Backend Package Structure

```
com.scholarops/
  ScholarOpsApplication.java           -- Spring Boot entry point

  config/
    SecurityConfig.java                -- SecurityFilterChain, BCrypt(12), method security
    CorsConfig.java                    -- CORS allowed origins for localhost
    AsyncConfig.java                   -- crawlExecutor and gradingExecutor thread pools
    EncryptionConfig.java              -- AES key injection
    JacksonConfig.java                 -- JSON serialization settings

  security/
    JwtTokenProvider.java              -- Access/refresh token generation and validation
    JwtAuthenticationFilter.java       -- OncePerRequestFilter for JWT extraction
    UserDetailsImpl.java               -- Spring Security UserDetails adapter
    UserDetailsServiceImpl.java        -- Loads user + roles + permissions from DB
    PasswordPolicyValidator.java       -- 5-rule password policy enforcement
    PermissionEvaluatorImpl.java       -- Custom @PreAuthorize permission evaluator

  controller/ (17 controllers)
    AuthController.java                -- POST /api/auth/login, /api/auth/refresh
    UserController.java                -- CRUD users, password reset, admin reset
    RoleController.java                -- Role assignment/revocation
    AuditLogController.java            -- Audit log queries
    CrawlSourceController.java         -- CRUD crawl source profiles
    CrawlRuleController.java           -- Rule version CRUD, activation, revert
    CrawlRunController.java            -- Start, monitor, cancel crawl runs
    ContentController.java             -- Content review and publish
    CatalogController.java             -- Public content catalog search
    QuestionBankController.java        -- Question bank and question CRUD
    QuizController.java                -- Quiz assembly, scheduling, publishing
    SubmissionController.java          -- Quiz taking, autosave, submit
    GradingController.java             -- Grading queue, grade items, rubric scores
    ScheduleController.java            -- CRUD schedule entries
    TimetableController.java           -- Move, merge, split, undo/redo, locked periods
    PlagiarismController.java          -- Plagiarism check results
    WrongAnswerController.java         -- Wrong answer history

  service/ (20 services)
    AuthService.java                   -- Login, token refresh, audit logging
    UserService.java                   -- User CRUD, password reset, admin reset
    RoleService.java                   -- Role assignment with permission change history
    AuditLogService.java               -- Audit log recording and querying
    EncryptionService.java             -- High-level encrypt/decrypt for credentials
    CrawlSourceService.java            -- Source profile management
    CrawlRuleService.java              -- Rule versioning with activation/revert
    CrawlRunService.java               -- Run lifecycle, async execution dispatch
    ParsingService.java                -- HTML parsing coordination
    ContentStandardizationService.java -- Raw data -> StandardizedContentRecord
    CatalogService.java                -- Full-text search, filtering, pagination
    QuestionBankService.java           -- Bank and question CRUD with tags
    QuizAssemblyService.java           -- Rule-based question selection, quiz creation
    SubmissionService.java             -- Quiz attempt lifecycle, autosave
    AutoGradingService.java            -- Objective answer auto-grading
    GradingWorkflowService.java        -- Queue routing, manual grading, rubric scoring
    ScheduleService.java               -- CRUD with journal and locked period checks
    TimetableService.java              -- Move/merge/split, undo/redo, locked periods
    PlagiarismService.java             -- Fingerprint-based similarity detection
    RateLimiterService.java            -- Token bucket rate limit management

  model/
    entity/ (24 JPA entities)          -- 1:1 mapping with database tables
    dto/ (25+ DTOs)                    -- Request/response data transfer objects
    enums/                             -- RoleName, PermissionType, AuditAction,
                                          CrawlStatus, ExtractionMethod, QuestionType,
                                          DifficultyLevel, GradingStatus,
                                          SubmissionStatus, ScheduleChangeType

  repository/ (17+ Spring Data JPA repositories)
    -- One per entity; custom queries for search, filtering, conflict detection

  exception/
    ResourceNotFoundException.java     -- 404
    UnauthorizedException.java         -- 401
    ForbiddenException.java            -- 403
    ConflictException.java             -- 409
    PasswordPolicyViolationException.java -- 400 with violation details
    RateLimitExceededException.java    -- 429 with Retry-After
    LockedPeriodConflictException.java -- 409 with conflict period details
    GlobalExceptionHandler.java        -- @RestControllerAdvice, maps all exceptions

  util/
    AesEncryptionUtil.java             -- AES-256-CBC encrypt/decrypt
    FingerprintUtil.java               -- N-gram fingerprinting for plagiarism
    AddressNormalizer.java             -- Location string normalization
    TimestampNormalizer.java           -- Timezone-aware timestamp standardization
    LanguageDetector.java              -- Content language detection

  crawler/
    RateLimiter.java                   -- Token bucket per source, ConcurrentHashMap
    XPathExtractor.java                -- XPath-based content extraction
    CssSelectorExtractor.java         -- CSS selector-based extraction
    RegexExtractor.java                -- Regex-based extraction
```

### 6.2 Frontend Module Structure

```
src/
  main.ts                             -- App bootstrap, Pinia, Router
  App.vue                             -- Root component

  router/
    index.ts                           -- Route definitions with meta.roles guards

  stores/
    auth.ts                            -- Pinia store: tokens, user, roles, permissions

  api/
    client.ts                          -- Axios instance with interceptors
    auth.ts                            -- Login, refresh, logout
    users.ts                           -- User CRUD
    crawl.ts                           -- Crawl sources, rules, runs
    content.ts                         -- Content review, publish
    quiz.ts                            -- Quiz assembly, management
    submissions.ts                     -- Quiz taking, autosave
    grading.ts                         -- Grading queue, grade items
    schedule.ts                        -- Schedule CRUD
    catalog.ts                         -- Content catalog search
    plagiarism.ts                      -- Plagiarism check results

  types/
    auth.ts, user.ts, crawl.ts,        -- TypeScript interfaces matching DTOs
    content.ts, quiz.ts, submission.ts,
    grading.ts, schedule.ts,
    catalog.ts, plagiarism.ts

  composables/
    useAuth.ts                         -- Authentication state and actions
    usePermission.ts                   -- Permission checking helpers
    useAutosave.ts                     -- 15s interval autosave with dirty tracking
    useCountdown.ts                    -- Quiz countdown timer with pause/resume
    useDragDrop.ts                     -- Timetable drag-and-drop
    useUndoRedo.ts                     -- Generic undo/redo stack (max 50 entries)

  guards/
    authGuard.ts                       -- Navigation guard: auth + role + permission checks
                                          (authGuard, guestGuard)

  utils/
    permissions.ts                     -- Permission constants and helpers
    formatters.ts                      -- Date, number, text formatters
    validators.ts                      -- Client-side validation rules

  components/
    layout/
      AppLayout.vue                    -- Main layout shell
      AppSidebar.vue                   -- Role-aware navigation sidebar
      AppHeader.vue                    -- Top bar with user info
      AppBreadcrumb.vue                -- Breadcrumb navigation
    common/
      LoadingSpinner.vue               -- Loading indicator
      EmptyState.vue                   -- Empty data placeholder
      ErrorDisplay.vue                 -- Error message display
      ForbiddenState.vue               -- 403 state
      ConfirmDialog.vue                -- Confirmation modal
      PaginationBar.vue                -- Pagination controls
      SearchFilterBar.vue              -- Search and filter controls
    admin/
      UserManagementTable.vue          -- User listing with CRUD
      RoleAssignmentModal.vue          -- Role assignment dialog
      PermissionAuditLog.vue           -- Permission change viewer
      AdminPasswordReset.vue           -- Admin password reset form

  views/ (organized by role)
    LoginView.vue, ForbiddenView.vue, NotFoundView.vue, DashboardView.vue
    admin/   -- UserManagementView, RoleManagementView, AuditHistoryView
    curator/ -- CrawlSourcesView, CrawlRulesView, CrawlRunsView, ContentReviewView
    instructor/ -- QuestionBanksView, QuizManagementView, QuizDetailView, SubmissionsReviewView
    student/ -- StudentDashboardView, CatalogView, TimetableView, AssessmentTakeView, WrongAnswerReviewView
    ta/      -- GradingQueueView, GradingDetailView
```

---

## 7. Key Workflows

### 7.1 User Authentication & Authorization

**Actors:** All users.
**Entry point:** `POST /api/auth/login`

```
1. Client submits {username, password} to AuthController.login()
2. AuthService.login() delegates to AuthenticationManager.authenticate()
   -> UserDetailsServiceImpl loads User + Roles + Permissions from DB
   -> BCryptPasswordEncoder(12) verifies the password hash
3. On success:
   a. JwtTokenProvider.generateAccessToken() builds JWT with sub, username, roles
   b. JwtTokenProvider.generateRefreshToken() builds refresh JWT with sub, type="refresh"
   c. User.lastLoginAt updated
   d. AuditLog entry: LOGIN_SUCCESS
   e. LoginResponse returned: accessToken, refreshToken, roles[], permissions[]
4. On failure:
   a. AuditLog entry: LOGIN_FAILURE (if user found)
   b. AuthenticationException propagated -> 401

Token Refresh:
1. Client sends refresh token to POST /api/auth/refresh
2. AuthService.refreshToken() validates the refresh token
3. Loads UserDetails by userId from token subject
4. Generates new access + refresh token pair
5. Returns updated LoginResponse
```

### 7.2 Crawl Job Configuration & Execution

**Actors:** Content Curator.
**Code path:** `CrawlSourceController` -> `CrawlRunController` -> `CrawlRunService`

```
1. Curator creates a crawl source profile:
   POST /api/crawl/sources -> CrawlSourceService.createSource()
   Stores: name, base_url, rate_limit_per_minute, requires_auth

2. If requires_auth, credentials are encrypted:
   EncryptionService -> AesEncryptionUtil.encrypt(plaintext, aesKey)
   -> Generates random 16-byte IV
   -> Stores encrypted_username, encrypted_password, encrypted_api_key,
      encryption_iv in encrypted_source_credentials

3. Curator creates extraction rule versions:
   POST /api/crawl/rules -> CrawlRuleService.createRule()
   Stores: source_profile_id, version_number (auto-incremented),
   extraction_method (XPATH/CSS_SELECTOR/REGEX),
   rule_definition (JSON), field_mappings (JSON)

4. Curator activates a rule version:
   PUT /api/crawl/rules/{id}/activate -> CrawlRuleService.activateVersion()
   Deactivates previous active version for the same source

5. Curator starts a crawl run:
   POST /api/crawl/runs -> CrawlRunService.startRun()
   a. Creates CrawlRun with status=PENDING
   b. Calls executeCrawlAsync(runId) via @Async
   c. Audit entry: CRAWL_RUN_START
   d. Returns immediately with run ID

6. Async execution (on crawlExecutor thread pool):
   a. Status -> RUNNING, sets started_at
   b. CrawlerEngine applies rate limiting via RateLimiter.tryAcquire()
      (token bucket: refills at rate_limit_per_minute / 60 tokens/sec)
   c. Extracts content using the appropriate extractor:
      - XPathExtractor for XPATH rules
      - CssSelectorExtractor for CSS_SELECTOR rules
      - RegexExtractor for REGEX rules
   d. Tracks: pages_crawled, pages_failed, items_extracted
   e. On success: status -> COMPLETED, sets completed_at
   f. On failure: status -> FAILED, stores error_log

7. Curator can cancel: PUT /api/crawl/runs/{id}/cancel
   -> Status -> CANCELLED (only if PENDING or RUNNING)
```

### 7.3 Content Parsing & Standardization Pipeline

**Actors:** Content Curator (post-crawl).
**Code path:** `ContentStandardizationService`, `ContentController`

```
1. Raw extracted data from crawl run is passed to:
   ContentStandardizationService.standardize(rawData, timezoneId, crawlRun, source)

2. Field mapping:
   a. title, description, bodyText, contentType, sourceUrl extracted from rawData map
   b. Timestamp normalization: TimestampNormalizer.normalize(rawTimestamp, timezoneId)
      -> Stores original_timestamp, standardized_timestamp, timezone_id
   c. Address normalization: AddressNormalizer.normalize(rawLocation)
      -> Stores original_location, normalized_address
   d. Language detection: LanguageDetector.detect(textContent)
      -> Stores detected_language
   e. Price parsing (if present)

3. Record saved to standardized_content_records with is_published=false

4. Curator reviews content:
   GET /api/content?page=0&size=20 -> lists unpublished content

5. Curator publishes selected content:
   POST /api/content/publish {contentIds: [1,2,3]}
   -> ContentStandardizationService.publishContent()
   -> Sets is_published=true, published_at, published_by
   -> Audit entry: CONTENT_PUBLISH per record

6. Published content becomes available in the student catalog
   via CatalogService (FULLTEXT search on title, description, body_text)
```

### 7.4 Quiz Assembly & Assessment

**Actors:** Instructor (assembly), Student (taking).
**Code path:** `QuizAssemblyService`, `SubmissionService`

```
Assembly (Instructor):
1. Instructor creates a question bank:
   POST /api/question-banks -> QuestionBankService.createBank()

2. Instructor adds questions with types, difficulty (1-5), options (JSON),
   correct answer, explanation, knowledge tags, and point values:
   POST /api/question-banks/{id}/questions -> QuestionBankService.addQuestion()

3. Instructor assembles quiz with rules:
   POST /api/quizzes -> QuizAssemblyService.assembleQuiz()
   a. Loads all questions from the specified question bank
   b. Applies DIFFICULTY rules: filters by difficulty_level, enforces min_count
   c. Fills remaining slots from unselected questions (shuffled)
   d. Validates total question count meets request
   e. Calculates total_points as sum of selected question points
   f. Creates QuizPaper with settings:
      time_limit_minutes, max_attempts, shuffle_questions,
      show_immediate_feedback, release_start, release_end
   g. Persists QuizRules for traceability
   h. Audit entry: QUIZ_CREATE

4. Instructor sets release window:
   PUT /api/quizzes/{id}/schedule -> QuizAssemblyService.scheduleQuiz()

5. Instructor publishes:
   PUT /api/quizzes/{id}/publish -> QuizAssemblyService.publishQuiz()

Assessment (Student):
6. Student starts quiz attempt:
   POST /api/submissions -> SubmissionService.startAttempt()
   -> Creates Submission with status=IN_PROGRESS, started_at,
      time_remaining_seconds = time_limit_minutes * 60

7. Frontend composable useCountdown(timeLimitSeconds, onExpire)
   starts a 1-second interval countdown timer

8. useAutosave composable fires every 15 seconds:
   PUT /api/submissions/{id}/autosave
   -> Saves current answers, updates auto_saved_at and time_remaining_seconds

9. Student submits:
   POST /api/submissions/{id}/submit
   -> status -> SUBMITTED, submitted_at set
   -> Triggers AutoGradingService.gradeObjectiveAnswers()
```

### 7.5 Grading Workflow (Auto + Manual)

**Actors:** System (auto-grading), Instructor, Teaching Assistant.
**Code path:** `AutoGradingService`, `GradingWorkflowService`

```
Auto-Grading (triggered on submission):
1. AutoGradingService.gradeObjectiveAnswers(submission) iterates all answers:
   a. For objective questions (SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE):
      - Compares student answer to correct_answer (case-insensitive trim)
      - Sets is_correct, auto_graded=true, score, graded_at
      - If incorrect: creates WrongAnswerHistory record with student answer,
        correct answer, and explanation
      - Accumulates total_score
   b. For subjective questions (SHORT_ANSWER, ESSAY):
      - Routes to grading queue: GradingWorkflowService.routeToQueue(answer)
      - Creates GradingState with status=PENDING
2. Submission status -> AUTO_GRADED

Manual Grading:
3. TA/Instructor views grading queue:
   GET /api/grading?status=PENDING
   -> GradingWorkflowService.getGradingQueue()

4. Grader assigns item (status -> ASSIGNED)

5. Grader reviews and grades:
   PUT /api/grading/{id}/grade {score, feedback}
   -> GradingWorkflowService.gradeItem()
   -> Updates GradingState: status=GRADED, score, feedback, graded_at
   -> Updates SubmissionAnswer: score, graded_at
   -> Audit entry: GRADING_SUBMIT

6. For detailed rubric grading:
   POST /api/grading/{id}/rubric-scores [{criterionName, maxScore, awardedScore, comment}]
   -> GradingWorkflowService.addRubricScores()
   -> Creates RubricScore entries per criterion
   -> Total awarded score = sum of all rubric awarded_scores
   -> Updates GradingState and SubmissionAnswer with total

Grading State Machine:
   PENDING -> ASSIGNED -> IN_REVIEW -> GRADED -> RETURNED
```

### 7.6 Timetable Editing with Change Journal

**Actors:** Student.
**Code path:** `ScheduleService`, `TimetableService`, `useUndoRedo`, `useDragDrop`

```
1. Student creates a schedule entry:
   POST /api/schedules -> ScheduleService.createSchedule()
   a. Checks locked period conflicts via TimetableService.checkLockedPeriodConflict()
      -> Queries lockedPeriodRepository.findConflicting(userId, start, end)
      -> Throws LockedPeriodConflictException with conflict details if overlap found
   b. Saves Schedule entity
   c. Records journal: changeType=CREATE, previous_state=null, new_state=JSON

2. Student drags schedule entry to new time (frontend useDragDrop composable):
   PUT /api/timetable/{id}/move {newStart, newEnd}
   -> TimetableService.moveSchedule()
   a. Validates ownership
   b. Checks locked period conflicts
   c. Snapshots current state
   d. Updates start_time, end_time
   e. Records journal: changeType=MOVE, previous_state=snapshot, new_state=updated

3. Merge two schedule entries:
   POST /api/timetable/merge {scheduleIds: [1, 2]}
   -> TimetableService.mergeSchedules()
   -> Sorts by start_time, creates merged entry, deletes originals

4. Split a schedule entry:
   POST /api/timetable/{id}/split {splitTime}
   -> TimetableService.splitSchedule()
   -> Creates two new entries, deletes original

5. Undo (frontend useUndoRedo composable + backend):
   POST /api/timetable/undo -> TimetableService.undo()
   a. Finds latest non-undone journal entry for the user
   b. Marks entry as is_undone=true
   c. Restores previous_state JSON back to schedule

6. Redo:
   POST /api/timetable/redo -> TimetableService.redo()
   a. Finds latest undone journal entry
   b. Marks is_undone=false
   c. Restores new_state JSON

Frontend UndoRedo composable (useUndoRedo.ts):
- Maintains in-memory undo/redo stacks (max 50 entries)
- Uses structuredClone for deep state copies
- Syncs with backend journal on each operation
```

### 7.7 Plagiarism Detection

**Actors:** System (triggered), Instructor (review).
**Code path:** `PlagiarismService`, `FingerprintUtil`

```
1. After submission, PlagiarismService.checkSubmission(submission) is invoked:
   a. Creates PlagiarismCheck with status=RUNNING
   b. Builds submission text by concatenating all answer_text fields
   c. Generates n-gram fingerprint:
      FingerprintUtil.generateFingerprint(text, ngramSize=5, windowSize=4)
   d. Compares against all prior submissions for the same quiz_paper_id:
      - Generates fingerprint for each prior submission
      - Computes Jaccard similarity via FingerprintUtil.computeSimilarity()
      - If similarity >= threshold (0.85): creates PlagiarismMatch
        with similarity_score, matched text excerpts (first 200 chars)
   e. Compares against all published standardized_content_records:
      - Same fingerprint + similarity computation on body_text
      - Creates PlagiarismMatch for content matches above threshold
   f. Sets max_similarity_score, is_flagged (if max >= 0.85)
   g. Status -> COMPLETED, checked_at set

2. Instructor views flagged submissions:
   GET /api/plagiarism/flagged -> PlagiarismService.getFlaggedChecks()

3. Instructor views match details:
   GET /api/plagiarism/{checkId}/matches -> PlagiarismService.getMatches()
   Returns: matched submission/content reference, similarity score, text excerpts

Configuration (application.yml):
  scholarops.plagiarism.similarity-threshold: 0.85
  scholarops.plagiarism.ngram-size: 5
  scholarops.plagiarism.window-size: 4
```

### 7.8 Admin Password Reset

**Actors:** Administrator.
**Code path:** `UserController` -> `UserService.adminResetPassword()`

```
1. Admin navigates to user management (AdminPasswordReset.vue component)
2. Admin selects target user and fills the reset form:
   - New password (must satisfy PasswordPolicyValidator)
   - Workstation ID (required, typically auto-populated from X-Workstation-Id header)
   - Reason (optional but recommended)

3. Request: POST /api/users/{userId}/admin-reset-password
   {
     newPassword: "...",
     workstationId: "WORKSTATION-LAB-01",
     reason: "User forgot password, verified via student ID"
   }

4. UserService.adminResetPassword():
   a. Validates workstationId is non-blank (IllegalArgumentException if missing)
   b. Loads target user from DB
   c. Validates new password against PasswordPolicyValidator.validate()
   d. Hashes new password with BCrypt(12)
   e. Updates user.password_hash
   f. Creates detailed audit log entry with:
      - Action: USER_ADMIN_PASSWORD_RESET
      - Details: "Admin password reset for user '{username}' by adminId={id}
                  from workstation='{workstationId}' at {timestamp}.
                  Reason: {reason}"
      - workstation_id column populated
   g. Logs at INFO level: admin ID, target user ID, workstation ID

5. Response: 200 OK (no password echoed back)

Key difference from self-service reset:
- Self-service (UserService.resetPassword()) requires old password verification
- Admin reset bypasses old password but requires:
  - PASSWORD_ADMIN_RESET permission
  - Workstation ID for accountability
  - Full audit trail with reason
```

---

## 8. Background Job Design

### 8.1 Crawl Execution (`CrawlRunService`)

**Thread Pool:** `crawlExecutor` (defined in `config/AsyncConfig.java`)
- Core pool size: 2
- Max pool size: 5
- Queue capacity: 25
- Thread name prefix: `crawl-`

**Execution model:**
- `CrawlRunService.executeCrawlAsync()` is annotated with `@Async` (defaults to `crawlExecutor`).
- Each crawl run executes on a pooled thread independent of the HTTP request thread.
- The calling method `startRun()` returns immediately after saving the PENDING run and dispatching the async task.

**Rate Limiting (`crawler/RateLimiter.java`):**
- Uses a `ConcurrentHashMap<Long, TokenBucket>` keyed by `sourceId`.
- Each `TokenBucket` is initialized with `tokensPerMinute` from the source profile's `rate_limit_per_minute` (default: 30).
- Refill rate: `tokensPerMinute / 60.0` tokens per second.
- Refill is computed from elapsed nanoseconds since last refill.
- `tryAcquire()` is synchronized per-bucket to prevent race conditions.
- Buckets can be reset via `reset(sourceId)` when a source profile is updated.

**Status Transitions:**
```
PENDING -> RUNNING -> COMPLETED
                   -> FAILED (on exception; error_log populated)
PENDING -> CANCELLED (via cancelRun())
RUNNING -> CANCELLED
```

### 8.2 Auto-Grading (`AutoGradingService`)

**Execution model:** Synchronous, triggered within the submission transaction.
- When a student submits, `SubmissionService` calls `AutoGradingService.gradeObjectiveAnswers()` within the same `@Transactional` boundary.
- No separate thread pool; grading completes before the HTTP response returns.
- Subjective items are routed to the grading queue (`GradingWorkflowService.routeToQueue()`) within the same transaction.

**Grading Executor Pool** (defined in `AsyncConfig.java`):
- Core pool size: 2
- Max pool size: 4
- Queue capacity: 50
- Thread name prefix: `grading-`
- Available for future async grading operations (e.g., batch re-grading).

### 8.3 Assessment Autosave

**Frontend implementation** (`composables/useAutosave.ts`):
- Parameterized composable: `useAutosave<T>(saveFn, intervalMs = 15000)`
- Uses `setInterval` with a default period of **15 seconds**.
- Tracks state via reactive refs: `isDirty`, `isSaving`, `lastSavedAt`, `error`.
- `markDirty(data)` sets the dirty flag and stores the current answer state.
- On each interval tick, `save()` checks `isDirty && !isSaving`, then calls the provided `saveFn`.
- On success: clears dirty flag, records `lastSavedAt`.
- On failure: stores error message for UI display.
- On component unmount (`onUnmounted`): performs a final save if dirty, then stops the timer.

**Backend configuration:**
- `scholarops.autosave.interval-seconds: 15` in `application.yml`
- Autosave endpoint: `PUT /api/submissions/{id}/autosave`
- Updates `auto_saved_at` and `time_remaining_seconds` on the `submissions` record.

### 8.4 Grading Queue State Machine

```
PENDING -----> ASSIGNED -----> IN_REVIEW -----> GRADED -----> RETURNED
   |                                               ^
   +----------- routeToQueue() --------------------+
                 (auto-created)         gradeItem() / addRubricScores()
```

- **PENDING:** Created by `GradingWorkflowService.routeToQueue()` when `AutoGradingService` encounters a subjective question.
- **ASSIGNED:** Set when a TA or Instructor claims the item from the queue.
- **IN_REVIEW:** Set when the grader begins reviewing the answer.
- **GRADED:** Set by `gradeItem()` or `addRubricScores()` with score, feedback, and graded_at.
- **RETURNED:** Set when the graded item is released back to the student for review.

States are defined in `model/enums/GradingStatus.java`:
```java
public enum GradingStatus {
    PENDING, ASSIGNED, IN_REVIEW, GRADED, RETURNED
}
```

---

## 9. Testing Strategy

### 9.1 Backend Unit Tests

**Framework:** JUnit 5 + Mockito

| Test Class | Coverage Area |
|---|---|
| `PasswordPolicyValidatorTest.java` | All 5 password rules, edge cases (null, empty, boundary lengths), valid passwords |
| `JwtTokenProviderTest.java` | Token generation, validation, expiration, claim extraction, malformed tokens |
| `PermissionEvaluatorTest.java` | Custom permission evaluation logic, role-based and permission-based checks |
| `AuthServiceTest.java` | Login success/failure, token refresh, audit logging, last_login_at update |
| `UserServiceTest.java` | User CRUD, duplicate detection, password policy enforcement, admin reset, soft delete |
| `CrawlRuleServiceTest.java` | Rule versioning, activation/deactivation, version number auto-increment |
| `ParsingServiceTest.java` | HTML parsing, extraction method dispatch |
| `ContentStandardizationServiceTest.java` | Field mapping, timestamp normalization, address normalization, language detection |
| `QuizAssemblyServiceTest.java` | Rule-based selection, insufficient questions error, point calculation, shuffle |

### 9.2 Backend Controller Tests

**Framework:** `@WebMvcTest` with MockMvc

Controller tests validate:
- HTTP status codes for success and error cases
- `@PreAuthorize` annotations block unauthorized access
- Request validation (`@Valid` annotations on DTOs)
- Response structure matches `ApiResponse<T>` / `PagedResponse<T>` formats
- `GlobalExceptionHandler` mapping (404, 401, 403, 409, 429, 400)

### 9.3 Backend Integration Tests

**Framework:** `@SpringBootTest` with test containers or embedded database

Integration tests cover:
- Full authentication flow from login through authorized API calls
- Flyway migration execution and schema validation (`ddl-auto: validate`)
- Transactional rollback behavior
- Async crawl execution on the `crawlExecutor` thread pool
- End-to-end quiz assembly -> submission -> auto-grading pipeline

### 9.4 Frontend Unit Tests

**Framework:** Vitest

Target composables and stores:
- `useAutosave` -- dirty tracking, interval firing, error handling, unmount save
- `useCountdown` -- start/pause/resume/reset, expiration callback, formatted output
- `useUndoRedo` -- push/undo/redo stack behavior, max history limit, reset
- `usePermission` -- role and permission checking
- `stores/auth` -- token storage, role/permission accessors, isAuthenticated computed
- `guards/authGuard` -- route guard logic for public/authenticated/role-restricted routes

### 9.5 Test Coverage by Domain

| Domain | Key Test Scenarios |
|---|---|
| **Authentication** | Valid login, invalid credentials, locked account, token expiration, refresh flow |
| **Password Policy** | Each rule individually, all rules combined, boundary values (11 vs 12 chars) |
| **RBAC** | Each role's permission set, permission escalation prevention, cross-role access denial |
| **Crawl Versioning** | Version increment, activation swaps, revert to prior version, concurrent rule edits |
| **Quiz Assembly** | Difficulty rule filtering, insufficient questions error, zero-rule assembly, point totals |
| **Grading** | Auto-grade correctness, wrong answer recording, subjective queue routing, rubric scoring |
| **Plagiarism** | Similarity above/below threshold, self-match exclusion, content corpus matching |
| **Timetable** | Create/move/merge/split, locked period conflict, undo/redo, journal sequence numbers |
| **Catalog** | Full-text search, pagination, published-only filtering |

---

## 10. Deployment Notes

### Docker Compose Configuration

The system is deployed via `docker/docker-compose.yml` with three services:

```
Service          Image              Port    Depends On
---------        -----              ----    ----------
mysql            mysql:8.0          3306    -
backend          (built)            8080    mysql (healthy)
frontend         (built)            80      backend
```

### Environment Variables

| Variable | Purpose | Default |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root password | `root_secret` |
| `DB_PASSWORD` | Application database password | `scholarops_secret` |
| `JWT_SECRET` | Base64-encoded HMAC signing key (>= 256 bits) | Development key (must override in production) |
| `AES_KEY` / `SCHOLAROPS_AES_KEY` | 32-character AES-256 key for credential encryption | `0123456789abcdef0123456789abcdef` |
| `CRAWL_STORAGE` | File path for crawl output storage | `/data/crawl-storage` |

### Startup Sequence

1. **MySQL** starts first; Docker Compose waits for the health check (`mysqladmin ping`) to pass (retries: 5, interval: 10s).
2. **Backend** starts after MySQL is healthy (`condition: service_healthy`):
   a. Flyway runs migrations from `classpath:db/migration` (baseline-on-migrate enabled).
   b. `V1__init_schema.sql` creates all 28 tables and seeds:
      - 5 roles (ADMINISTRATOR, CONTENT_CURATOR, INSTRUCTOR, TEACHING_ASSISTANT, STUDENT)
      - 19 permissions across 6 categories
      - Role-permission mappings
      - Default admin user (`admin` / `Admin@12345678`, BCrypt-12 hashed)
      - Admin user assigned ADMINISTRATOR role
   c. Hibernate validates schema (`ddl-auto: validate`).
   d. HikariCP connection pool initializes (min-idle: 5, max: 20).
   e. Spring Boot starts on port 8080.
3. **Frontend** starts after backend:
   a. nginx serves the built Vue.js SPA on port 80.
   b. Proxies `/api/` requests to `http://backend:8080/api/`.

### Volumes

- `mysql-data` -- persistent MySQL data directory
- `crawl-storage` -- persistent crawl output files

### Network

All three services communicate over the `scholarops-net` bridge network. The frontend nginx container references the backend by Docker service name (`backend`).

### Offline Operation

- No external DNS, CDN, or third-party API calls are required.
- All frontend assets are bundled at build time and served from nginx.
- The MySQL instance is local. No cloud database connections.
- Crawl operations target URLs on the LAN or localhost (configurable per source profile).
- Authentication is self-contained (JWT signing key is local).

---

## 11. Manual Verification Required

The following behaviors cannot be fully validated through automated unit or integration tests and require manual verification in a realistic deployment environment:

1. **Browser drag-and-drop timetable behavior.** The `useDragDrop` composable coordinates visual drag operations with the backend `TimetableService.moveSchedule()` API. Verify that drag events, drop targets, visual feedback, and conflict error messages work correctly across Chrome, Firefox, and Safari.

2. **Real-time countdown timer accuracy under tab switching.** The `useCountdown` composable uses `setInterval(fn, 1000)`. Browsers throttle intervals in background tabs. Verify that `time_remaining_seconds` from the backend autosave is used to resynchronize the timer when the tab regains focus, and that quiz auto-submission fires correctly even after prolonged background tab periods.

3. **Autosave network retry behavior under intermittent connectivity.** The `useAutosave` composable captures errors but does not implement exponential backoff or retry. Verify behavior when the backend is temporarily unreachable: confirm that the dirty flag is preserved, error messages display, and the next interval tick reattempts the save.

4. **Crawl execution against real URLs with rate limiting.** The `RateLimiter` token bucket is tested in isolation, but full end-to-end crawl behavior (HTTP fetching, HTML parsing, extraction rule application, rate-limited page traversal) requires testing against actual or mock HTTP servers to validate timing, error handling, and content extraction fidelity.

5. **Docker container orchestration startup sequence.** Verify that the health check timing is sufficient for MySQL initialization on the target hardware, that Flyway migrations complete before the backend accepts traffic, and that the frontend nginx proxy correctly resolves the `backend` hostname after startup.

6. **AES encryption key rotation procedure.** The system uses a single AES key from the environment variable. Verify the operational procedure for key rotation: re-encrypting all existing `encrypted_source_credentials` rows with a new key, updating the environment variable, and restarting the backend without data loss or downtime.

7. **Full penetration testing.** While the system implements JWT authentication, BCrypt hashing, CORS restrictions, and security headers, a formal penetration test should validate: SQL injection resistance, XSS prevention, CSRF protection adequacy (given stateless JWT), JWT algorithm confusion attacks, and authorization bypass attempts across all 17 controller endpoints.

8. **Rate limiter timing precision under high concurrency.** The `TokenBucket.tryConsume()` method is `synchronized` per bucket, but the refill calculation uses `System.nanoTime()`. Under high concurrency with many simultaneous crawl jobs, verify that token refill timing remains accurate and that no starvation or over-provisioning occurs.

9. **BCrypt cost factor performance impact.** The system uses BCrypt with cost factor 12 (`new BCryptPasswordEncoder(12)`). On the target deployment hardware, verify that login response times remain acceptable (BCrypt-12 typically takes 200-400ms per hash). If the hardware is constrained, measure whether this introduces unacceptable latency under concurrent login load.
