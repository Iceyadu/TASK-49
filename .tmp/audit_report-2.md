# ScholarOps Static Delivery Acceptance & Architecture Audit

## 1. Verdict
- Overall conclusion: **Partial Pass**
- Basis: the codebase demonstrates substantial alignment with the prompt (multi-role RBAC, crawl/rules, standardization, assessments, grading, timetable, catalog) and has broad static test and wiring artifacts, but **material gaps remain** in end-to-end completeness, some FE↔BE contracts, placeholder UIs, object-level authorization depth, and sensitive-data exposure confidence—so delivery is **not** a full static **Pass**, yet it is **above** a wholesale **Fail** given real modules, migrations, and security baselines present.

## 2. Scope and Static Verification Boundary
- Reviewed:
  - Backend Spring Boot code, security config, controllers/services/entities, Flyway schema, and tests.
  - Frontend Vue routes/views/components/stores/API clients and tests.
  - Project docs (`repo/README.md`, `docs/api-spec.md`) and test scripts (`repo/run_tests.sh`, `repo/API_tests/*`, `repo/unit_tests/*`).
- Not reviewed:
  - Runtime behavior under a real DB/browser/network.
  - Deployment/runtime performance, concurrency behavior, and UX rendering fidelity.
- Intentionally not executed:
  - No app startup, no tests, no Docker, no external services.
- Claims requiring manual verification:
  - Any runtime startup, API response shape after serialization, async crawl execution correctness, frontend rendering/interactions in browser, and real security posture under live traffic.

## 3. Repository / Requirement Mapping Summary
- Prompt core goals mapped:
  - Offline multi-role RBAC platform, content intake/crawling with rule versioning/testing/revert, standardization pipeline, assessments and grading workflows, timetable editing (merge/split/lock/undo-redo), and student catalog/search + wrong-answer review.
- Main implementation areas mapped:
  - Auth/RBAC (`SecurityConfig`, JWT, `@PreAuthorize` controllers).
  - Crawl/rules/runs (`CrawlSource*`, `CrawlRule*`, `CrawlRun*`, crawler engine).
  - Standardization/parsing (`ParsingService`, `ContentStandardizationService`, normalizers).
  - Quiz/submission/grading/timetable/catalog modules.
  - Vue role routes/workspaces and API integration layers.

## 4. Section-by-section Review

### 4.1 Hard Gates

#### 1.1 Documentation and static verifiability
- Conclusion: **Partial Pass**
- Rationale: project has run/test docs and clear structure, but doc-to-code contradictions and major wiring/contract defects reduce static verifiability confidence.
- Evidence:
  - Run/test docs exist: `repo/README.md:8-25`, `repo/README.md:40-48`
  - Test runner exists: `repo/run_tests.sh:21-215`
  - API spec mismatch with implementation (login email vs username; logout invalidation claim): `docs/api-spec.md:98-102`, `docs/api-spec.md:168-170`, `repo/backend/src/main/java/com/scholarops/model/dto/LoginRequest.java:15-19`, `repo/backend/src/main/java/com/scholarops/controller/AuthController.java:38-41`
- Manual verification note: startup viability requires runtime confirmation due unresolved bean wiring risk.

#### 1.2 Material deviation from Prompt
- Conclusion: **Fail**
- Rationale: several prompt-critical flows are incomplete or mismatched (student wrong-answer flow, grading workspace completeness, assessment feedback semantics, frontend/backend payload contracts).
- Evidence:
  - Placeholder views with no live data wiring: `repo/frontend/src/views/ta/GradingQueueView.vue:5`, `repo/frontend/src/views/ta/GradingDetailView.vue:4-9`, `repo/frontend/src/views/instructor/SubmissionsReviewView.vue:5`, `repo/frontend/src/views/student/WrongAnswerReviewView.vue:5`
  - Student dashboard “Assessments” pulls catalog items, not quiz list: `repo/frontend/src/views/student/StudentDashboardView.vue:39`
  - Assembly payload mismatch: `repo/frontend/src/api/quiz.ts:5-7`, `repo/backend/src/main/java/com/scholarops/model/dto/QuizAssemblyRequest.java:29-35`
  - Rubric payload mismatch: `repo/frontend/src/api/grading.ts:21-23`, `repo/backend/src/main/java/com/scholarops/controller/GradingController.java:52-56`

### 4.2 Delivery Completeness

#### 2.1 Core explicit requirements coverage
- Conclusion: **Fail**
- Rationale: many core areas exist, but key explicit requirements are not fully met end-to-end.
- Evidence:
  - RBAC/menu+API structure exists: `repo/backend/src/main/java/com/scholarops/config/SecurityConfig.java:40-43`, `repo/frontend/src/router/index.ts:16-37`
  - But frontend permission model diverges from backend permission codes: `repo/frontend/src/utils/permissions.ts:4-75`, `repo/backend/src/main/resources/db/migration/V1__init_schema.sql:491-510`, `repo/backend/src/main/java/com/scholarops/security/UserDetailsServiceImpl.java:56-60`
  - Wrong-answer review exists server-side but UI is static empty list: `repo/backend/src/main/java/com/scholarops/controller/WrongAnswerController.java:24-39`, `repo/frontend/src/views/student/WrongAnswerReviewView.vue:5`
  - Prompt-required autosave cadence mismatch: configured 15s but assessment UI autosaves on 3s input debounce: `repo/backend/src/main/resources/application.yml:51-52`, `repo/frontend/src/components/student/AssessmentView.vue:194-200`

#### 2.2 End-to-end 0→1 deliverable vs partial/demo
- Conclusion: **Partial Pass**
- Rationale: repository is full-stack with substantial modules, but several user-critical surfaces are still placeholder/demo-like.
- Evidence:
  - Complete backend/frontend structure and migrations/tests: `repo/README.md:3-6`, `repo/backend/src/main/resources/db/migration/V1__init_schema.sql:482-540`, `repo/frontend/package.json:6-13`
  - Demo-like placeholders in production routes: `repo/frontend/src/views/ta/GradingQueueView.vue:5`, `repo/frontend/src/views/student/WrongAnswerReviewView.vue:5`

### 4.3 Engineering and Architecture Quality

#### 3.1 Structure and module decomposition
- Conclusion: **Partial Pass**
- Rationale: modules are separated by domain; however, architectural inconsistencies and duplicate rate-limit implementations increase risk.
- Evidence:
  - Clear domain service/controller split: `repo/backend/src/main/java/com/scholarops/controller/QuizController.java:20-22`, `repo/backend/src/main/java/com/scholarops/service/QuizAssemblyService.java:18-20`
  - Duplicate rate limiting logic in two implementations: `repo/backend/src/main/java/com/scholarops/service/RateLimiterService.java:9-22`, `repo/backend/src/main/java/com/scholarops/crawler/RateLimiter.java:6-19`

#### 3.2 Maintainability/extensibility
- Conclusion: **Fail**
- Rationale: hard contract drift between layers and non-bean utility injection into a Spring service indicate brittle architecture.
- Evidence:
  - Non-bean utility classes injected as constructor dependencies: `repo/backend/src/main/java/com/scholarops/service/ContentStandardizationService.java:26-35`
  - Utility classes are `final` with private constructors and no Spring annotations/beans: `repo/backend/src/main/java/com/scholarops/util/TimestampNormalizer.java:8`, `repo/backend/src/main/java/com/scholarops/util/TimestampNormalizer.java:35-37`, `repo/backend/src/main/java/com/scholarops/util/AddressNormalizer.java:8`, `repo/backend/src/main/java/com/scholarops/util/AddressNormalizer.java:100-102`, `repo/backend/src/main/java/com/scholarops/util/LanguageDetector.java:5`, `repo/backend/src/main/java/com/scholarops/util/LanguageDetector.java:47-49`

### 4.4 Engineering Details and Professionalism

#### 4.1 Error handling, logging, validation, API design
- Conclusion: **Partial Pass**
- Rationale: strong exception envelope and validation coverage exist, but sensitive-data exposure risk and contract errors are material.
- Evidence:
  - Global exception handling with consistent response envelopes: `repo/backend/src/main/java/com/scholarops/exception/GlobalExceptionHandler.java:22-126`
  - Password policy and BCrypt strength are implemented: `repo/backend/src/main/java/com/scholarops/security/PasswordPolicyValidator.java:13-43`, `repo/backend/src/main/java/com/scholarops/config/SecurityConfig.java:50-53`
  - Risky entity exposure at API boundary (users/crawl source): `repo/backend/src/main/java/com/scholarops/controller/UserController.java:29-46`, `repo/backend/src/main/java/com/scholarops/model/entity/User.java:23-24`, `repo/backend/src/main/java/com/scholarops/controller/CrawlSourceController.java:29-47`, `repo/backend/src/main/java/com/scholarops/model/entity/CrawlSourceProfile.java:46-47`, `repo/backend/src/main/java/com/scholarops/model/entity/EncryptedSourceCredential.java:19-29`

#### 4.2 Product/service realism vs demo
- Conclusion: **Partial Pass**
- Rationale: overall codebase resembles a real service, but key role workspaces remain static placeholders.
- Evidence:
  - Real domain persistence + security + migration stack: `repo/backend/pom.xml:28-118`, `repo/backend/src/main/resources/db/migration/V1__init_schema.sql:482-540`
  - Placeholder role views: `repo/frontend/src/views/ta/GradingDetailView.vue:4-9`, `repo/frontend/src/views/instructor/SubmissionsReviewView.vue:5`

### 4.5 Prompt Understanding and Requirement Fit

#### 5.1 Business goal and constraints fit
- Conclusion: **Fail**
- Rationale: partial alignment exists, but prompt-critical semantics are violated in security and student assessment experience.
- Evidence:
  - Student can retrieve quiz object containing `correctAnswer` fields (leaks objective answers): `repo/backend/src/main/java/com/scholarops/controller/QuizController.java:46-50`, `repo/backend/src/main/java/com/scholarops/service/QuizAssemblyService.java:115-118`, `repo/backend/src/main/java/com/scholarops/model/entity/Question.java:34-35`, `repo/frontend/src/components/student/AssessmentView.vue:171-173`
  - Wrong-answer review linkage to instructor explanation not implemented end-to-end in UI route: `repo/frontend/src/views/student/WrongAnswerReviewView.vue:5`
  - Timetable split action passes schedule start time (violates service precondition): `repo/frontend/src/views/student/TimetableView.vue:77`, `repo/backend/src/main/java/com/scholarops/service/TimetableService.java:78-80`

### 4.6 Aesthetics (frontend)

#### 6.1 Visual/interaction quality
- Conclusion: **Cannot Confirm Statistically**
- Rationale: static CSS indicates basic hierarchy and hover states, but visual quality/render correctness must be browser-verified.
- Evidence:
  - Sidebar and interaction styles present: `repo/frontend/src/components/layout/AppSidebar.vue:183-204`
  - Component hover/disabled states present in timetable editor: `repo/frontend/src/components/student/TimetableEditor.vue:228-235`
- Manual verification note: render and interaction quality across desktop/mobile require browser inspection.

## 5. Issues / Suggestions (Severity-Rated)

### Blocker

1) Severity: **Blocker**
- Title: Spring bean wiring is statically inconsistent in content standardization service
- Conclusion: **Fail**
- Evidence:
  - Constructor expects injected `TimestampNormalizer`, `AddressNormalizer`, `LanguageDetector`: `repo/backend/src/main/java/com/scholarops/service/ContentStandardizationService.java:26-35`
  - Utility classes are non-beans with private constructors: `repo/backend/src/main/java/com/scholarops/util/TimestampNormalizer.java:8`, `repo/backend/src/main/java/com/scholarops/util/TimestampNormalizer.java:35-37`, `repo/backend/src/main/java/com/scholarops/util/AddressNormalizer.java:8`, `repo/backend/src/main/java/com/scholarops/util/AddressNormalizer.java:100-102`, `repo/backend/src/main/java/com/scholarops/util/LanguageDetector.java:5`, `repo/backend/src/main/java/com/scholarops/util/LanguageDetector.java:47-49`
- Impact: backend startup/context creation is at high risk of failing around this service wiring.
- Minimum actionable fix: convert utilities to Spring beans (or remove constructor injection and call static methods directly), then update tests accordingly.

### High

2) Severity: **High**
- Title: Student quiz retrieval can expose correct answers before submission
- Conclusion: **Fail**
- Evidence:
  - Student role allowed on `/api/quizzes/{id}`: `repo/backend/src/main/java/com/scholarops/controller/QuizController.java:47-50`
  - Service returns full `QuizPaper` entity directly: `repo/backend/src/main/java/com/scholarops/service/QuizAssemblyService.java:115-118`
  - `Question` entity includes `correctAnswer`: `repo/backend/src/main/java/com/scholarops/model/entity/Question.java:34-35`
- Impact: assessment integrity can be bypassed; objective answers may be disclosed.
- Minimum actionable fix: add student-safe DTO that excludes `correctAnswer`/grading internals and enforce publication/window checks for student fetch.

3) Severity: **High**
- Title: Frontend/backend contract mismatch breaks quiz assembly flow
- Conclusion: **Fail**
- Evidence:
  - Frontend sends `{title, rules, bankId}`: `repo/frontend/src/api/quiz.ts:5-7`
  - Backend requires `questionBankId` and `totalQuestions`: `repo/backend/src/main/java/com/scholarops/model/dto/QuizAssemblyRequest.java:29-35`
  - UI collects `totalQuestions` but call ignores it and hardcodes bank ID: `repo/frontend/src/views/instructor/QuizManagementView.vue:64-68`
- Impact: quiz assembly request likely fails validation or creates incorrect behavior.
- Minimum actionable fix: align payload names/fields (`questionBankId`, `totalQuestions`, and other required options), and propagate selected bank from UI.

4) Severity: **High**
- Title: Grading rubric API payload mismatch
- Conclusion: **Fail**
- Evidence:
  - Frontend posts `{ rubricScores }`: `repo/frontend/src/api/grading.ts:21-23`
  - Backend expects raw list body `List<RubricScoreRequest>`: `repo/backend/src/main/java/com/scholarops/controller/GradingController.java:52-56`
- Impact: rubric-scoring endpoint likely returns 400 for valid UI usage.
- Minimum actionable fix: send raw array from frontend or adapt backend contract to accept wrapper DTO.

5) Severity: **High**
- Title: JWT refresh/logout token lifecycle is weak
- Conclusion: **Fail**
- Evidence:
  - Refresh validates generic token only, no refresh-type check at service boundary: `repo/backend/src/main/java/com/scholarops/service/AuthService.java:129-135`, `repo/backend/src/main/java/com/scholarops/security/JwtTokenProvider.java:86-99`
  - Logout endpoint is no-op success response: `repo/backend/src/main/java/com/scholarops/controller/AuthController.java:38-41`
- Impact: token misuse/replay risk remains; logout does not invalidate refresh credentials.
- Minimum actionable fix: enforce token-type verification for refresh and implement refresh-token store/revocation/rotation.

6) Severity: **High**
- Title: Sensitive/internal fields can leak via direct entity responses
- Conclusion: **Fail**
- Evidence:
  - User API returns `User` entities directly: `repo/backend/src/main/java/com/scholarops/controller/UserController.java:29-46`
  - `User` contains `passwordHash`: `repo/backend/src/main/java/com/scholarops/model/entity/User.java:23-24`
  - Crawl source API returns entities with `credential` relation: `repo/backend/src/main/java/com/scholarops/controller/CrawlSourceController.java:29-47`, `repo/backend/src/main/java/com/scholarops/model/entity/CrawlSourceProfile.java:46-47`, `repo/backend/src/main/java/com/scholarops/model/entity/EncryptedSourceCredential.java:19-29`
- Impact: credential and password hash exposure risk through serialization/expansion.
- Minimum actionable fix: enforce strict DTOs for all API responses and mark sensitive entity fields with ignore annotations as defense-in-depth.

7) Severity: **High**
- Title: Frontend permission model diverges from backend permission codes
- Conclusion: **Fail**
- Evidence:
  - Frontend permission strings use `user:view` style: `repo/frontend/src/utils/permissions.ts:4-75`
  - Backend authorizations and seeded permissions use `USER_MANAGE`/`CONTENT_VIEW` style: `repo/backend/src/main/resources/db/migration/V1__init_schema.sql:491-510`, `repo/backend/src/main/java/com/scholarops/security/UserDetailsServiceImpl.java:56-60`
  - Route guard does exact inclusion checks: `repo/frontend/src/guards/authGuard.ts:34-37`, `repo/frontend/src/stores/auth.ts:14`
- Impact: navigation/action authorization can deny valid users or misalign with backend policy intent.
- Minimum actionable fix: unify permission vocabulary end-to-end and enforce one canonical permission enum shared across FE/BE.

8) Severity: **High**
- Title: Prompt-critical role workspaces are placeholder-only in UI
- Conclusion: **Fail**
- Evidence:
  - TA queue/detail static empty placeholders: `repo/frontend/src/views/ta/GradingQueueView.vue:5`, `repo/frontend/src/views/ta/GradingDetailView.vue:4-9`
  - Instructor submissions review static empty: `repo/frontend/src/views/instructor/SubmissionsReviewView.vue:5`
  - Wrong-answer view static empty: `repo/frontend/src/views/student/WrongAnswerReviewView.vue:5`
- Impact: key workflows required by prompt are not materially delivered in user-facing application.
- Minimum actionable fix: connect each view to corresponding APIs and handle loading/error/empty states with real data models.

9) Severity: **High**
- Title: Default JWT/AES secrets are embedded in source/config defaults
- Conclusion: **Fail**
- Evidence:
  - JWT/AES defaults in application config: `repo/backend/src/main/resources/application.yml:38-43`
  - Same defaults in compose env: `repo/docker-compose.yml:40-42`
- Impact: predictable secrets increase compromise risk if defaults persist in deployments.
- Minimum actionable fix: remove insecure defaults, fail fast when missing, and require explicit secure environment-provided secrets.

### Medium

10) Severity: **Medium**
- Title: Assessment autosave behavior mismatches 15-second requirement
- Conclusion: **Fail**
- Evidence:
  - Prompt-aligned interval configured at 15s: `repo/backend/src/main/resources/application.yml:51-52`
  - Active assessment uses 3s input debounce instead: `repo/frontend/src/components/student/AssessmentView.vue:194-200`
- Impact: requirement non-conformance and inconsistent autosave semantics.
- Minimum actionable fix: implement stable 15-second periodic autosave in assessment flow (or document intentional change and rationale).

11) Severity: **Medium**
- Title: Timetable split and rendering logic have static correctness defects
- Conclusion: **Fail**
- Evidence:
  - Split uses `startTime` as split point from UI: `repo/frontend/src/views/student/TimetableView.vue:77`
  - Service requires split strictly between start/end: `repo/backend/src/main/java/com/scholarops/service/TimetableService.java:78-80`
  - Session style parses ISO timestamps via naive `split(':')`: `repo/frontend/src/components/student/TimetableEditor.vue:161-164`
- Impact: split can systematically fail; schedule rendering positions can be incorrect.
- Minimum actionable fix: use validated midpoint/selected split time and robust date parsing for style calculations.

12) Severity: **Medium**
- Title: Catalog pagination count query ignores most active filters
- Conclusion: **Fail**
- Evidence:
  - Search query applies type/price/date filters: `repo/backend/src/main/java/com/scholarops/service/CatalogService.java:42-56`
  - Count query only applies published+keyword: `repo/backend/src/main/java/com/scholarops/service/CatalogService.java:79-88`
- Impact: total counts/pages can be wrong; UI pagination/filter UX becomes inconsistent.
- Minimum actionable fix: build count predicates from the same filter set as main query.

13) Severity: **Medium**
- Title: Crawl credential encryption flow is incomplete for authenticated-source crawling
- Conclusion: **Partial Fail**
- Evidence:
  - Credentials are encrypted/saved: `repo/backend/src/main/java/com/scholarops/service/CrawlSourceService.java:169-208`
  - Crawler fetch path uses plain `Jsoup.connect(url)` with no credential usage: `repo/backend/src/main/java/com/scholarops/crawler/CrawlerEngine.java:144-149`
- Impact: `requiresAuth` sources likely cannot be crawled despite stored credentials.
- Minimum actionable fix: add secure credential retrieval/decryption and authenticated request support in crawler execution path.

14) Severity: **Medium**
- Title: Documentation and tests contain stale/contradictory API contracts
- Conclusion: **Partial Fail**
- Evidence:
  - API spec login request documents `email` while backend requires `username`: `docs/api-spec.md:98-108`, `repo/backend/src/main/java/com/scholarops/model/dto/LoginRequest.java:15-19`
  - Frontend contract test asserts old publish method behavior: `repo/frontend/tests/unit/api/quizApiContract.spec.ts:95-106`, while client uses PUT: `repo/frontend/src/api/quiz.ts:25-27`
- Impact: verification and onboarding become error-prone; false confidence from stale contract tests.
- Minimum actionable fix: update API spec/tests to current contract and add CI checks for contract drift.

## 6. Security Review Summary

- Authentication entry points: **Partial Pass**
  - Evidence: JWT filter + protected `/api/**`: `repo/backend/src/main/java/com/scholarops/config/SecurityConfig.java:40-43`, `repo/backend/src/main/java/com/scholarops/security/JwtAuthenticationFilter.java:36-45`
  - Gap: refresh token-type/revocation weaknesses: `repo/backend/src/main/java/com/scholarops/service/AuthService.java:129-141`, `repo/backend/src/main/java/com/scholarops/controller/AuthController.java:38-41`.

- Route-level authorization: **Pass**
  - Evidence: pervasive `@PreAuthorize` on controllers (examples): `repo/backend/src/main/java/com/scholarops/controller/UserController.java:27-84`, `repo/backend/src/main/java/com/scholarops/controller/GradingController.java:29-58`, `repo/backend/src/main/java/com/scholarops/controller/ScheduleController.java:34-96`.

- Object-level authorization: **Fail**
  - Evidence: grading queue/state operations lack assignee/owner checks in service layer: `repo/backend/src/main/java/com/scholarops/service/GradingWorkflowService.java:42-44`, `repo/backend/src/main/java/com/scholarops/service/GradingWorkflowService.java:52-101`.

- Function-level authorization: **Partial Pass**
  - Evidence: method security and permission evaluator are wired: `repo/backend/src/main/java/com/scholarops/config/SecurityConfig.java:60-64`, `repo/backend/src/main/java/com/scholarops/security/PermissionEvaluatorImpl.java:13-29`
  - Gap: frontend permission constants diverge from backend permission codes: `repo/frontend/src/utils/permissions.ts:4-75`, `repo/backend/src/main/resources/db/migration/V1__init_schema.sql:491-510`.

- Tenant/user data isolation: **Partial Pass**
  - Evidence: submission/timetable own-resource checks exist: `repo/backend/src/main/java/com/scholarops/service/SubmissionService.java:156-161`, `repo/backend/src/main/java/com/scholarops/service/TimetableService.java:176-181`
  - Gap: quiz fetch for students can expose sensitive question fields regardless of ownership/context: `repo/backend/src/main/java/com/scholarops/controller/QuizController.java:47-50`, `repo/backend/src/main/java/com/scholarops/model/entity/Question.java:34-35`.

- Admin/internal/debug endpoint protection: **Pass**
  - Evidence: admin endpoints guarded via role+permission: `repo/backend/src/main/java/com/scholarops/controller/AuditLogController.java:27-43`, `repo/backend/src/main/java/com/scholarops/controller/RoleController.java:26-47`
  - No explicit debug/internal endpoint exposure found statically in reviewed source tree.

## 7. Tests and Logging Review

- Unit tests: **Partial Pass**
  - Exists and broad module coverage: `repo/backend/src/test/java/com/scholarops/service/*`, `repo/frontend/tests/unit/*`
  - Gap: some tests encode stale contracts and mock too high to catch service-level auth/data leaks: `repo/frontend/tests/unit/api/quizApiContract.spec.ts:95-106`, `repo/backend/src/test/java/com/scholarops/controller/SubmissionAuthorizationTest.java:54-57`.

- API/integration tests: **Partial Pass**
  - curl-based and integration test artifacts exist: `repo/run_tests.sh:59-167`, `repo/API_tests/test_auth_endpoints.sh:21-67`, `repo/API_tests/test_submission_ownership.sh:126-154`
  - Static boundary: cannot confirm real pass/fail without execution.

- Logging categories/observability: **Partial Pass**
  - Logging config present: `repo/backend/src/main/resources/application.yml:54-59`
  - Domain logs exist across services: `repo/backend/src/main/java/com/scholarops/service/AuthService.java:149`, `repo/backend/src/main/java/com/scholarops/crawler/CrawlerEngine.java:46-47`.

- Sensitive-data leakage risk in logs/responses: **Fail**
  - Response risk: direct entities may serialize sensitive fields (`passwordHash`, encrypted credential blobs): `repo/backend/src/main/java/com/scholarops/model/entity/User.java:23-24`, `repo/backend/src/main/java/com/scholarops/model/entity/EncryptedSourceCredential.java:19-29`
  - Log risk: failed login audit stores exception message text directly: `repo/backend/src/main/java/com/scholarops/service/AuthService.java:118`.

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview
- Unit tests exist:
  - Backend: JUnit via Maven (`spring-boot-starter-test`, `spring-security-test`): `repo/backend/pom.xml:104-117`
  - Frontend: Vitest: `repo/frontend/package.json:10-12`, `repo/frontend/tests/vitest.config.ts:1-42`
- API/integration tests exist:
  - Shell-based API tests and orchestrator: `repo/run_tests.sh:59-205`, `repo/API_tests/test_401_protected_endpoints.sh:1-57`
- Test entry points documented:
  - `repo/README.md:40-48`, `repo/run_tests.sh:196-205`
- Static boundary:
  - Tests were not executed in this audit.

### 8.2 Coverage Mapping Table

| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture / Mock | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| Unauthenticated access returns 401 on protected APIs | `repo/API_tests/test_401_protected_endpoints.sh:28-55` | Expects 401 for `/api/users`, `/api/roles`, `/api/catalog`, etc. | sufficient | Runtime not executed in this audit | Execute in CI against ephemeral environment |
| Role boundary (student cannot grade) | `repo/backend/src/test/java/com/scholarops/controller/SubmissionAuthorizationTest.java:143-188` | `@WithMockUser(roles="STUDENT")` expects 403 on grading routes | basically covered | Controller-web tests mock services; service-level auth defects can escape | Add service/integration tests validating DB-backed ownership and assignee checks |
| Submission ownership isolation | `repo/API_tests/test_submission_ownership.sh:126-143` | Student B must receive 403/404 on Student A submission | basically covered | Not executed; instructor/TA edge cases still sparse | Add deterministic fixture-based integration tests for ownership matrices |
| Password policy enforcement | `repo/backend/src/test/java/com/scholarops/security/PasswordPolicyValidatorTest.java` + `repo/API_tests/test_auth_endpoints.sh:21-29` | Validation of strong/weak passwords and auth responses | basically covered | No explicit regression around admin-reset policy path | Add admin-reset password policy integration test |
| Refresh token security (type/revocation) | `repo/backend/src/test/java/com/scholarops/service/AuthServiceTest.java:81-96` | Only happy-path refresh mocked via `validateToken=true` | insufficient | No test that access token cannot refresh; no revocation/logout checks | Add tests for token type claim enforcement and post-logout refresh rejection |
| Quiz assembly contract FE↔BE | `repo/frontend/tests/unit/api/quizApiContract.spec.ts:41-51` | Asserts FE sends `{title,rules,bankId}` | insufficient | Test validates wrong contract against backend DTO | Add contract test asserting backend-compatible payload (`questionBankId`,`totalQuestions`) |
| Objective answer confidentiality | none found | N/A | missing | No test blocks student exposure of `correctAnswer` in quiz-fetch payload | Add API test to assert student quiz payload excludes `correctAnswer` |
| Timetable lock/split/undo flows | `repo/backend/src/test/java/com/scholarops/service/TimetableServiceTest.java`, `repo/API_tests/test_schedule_timetable.sh` | Tests exist for timetable service and API scripts | basically covered | No FE test for invalid split-time generation from UI | Add frontend unit test for split time selection and ISO parsing correctness |
| Sensitive response leakage | `repo/API_tests/test_sensitive_data_leakage.sh:126-145` | Greps for `passwordHash`/bcrypt in user APIs | insufficient | Does not check quiz payload answer leakage or crawl credential exposure | Extend leakage tests to `/api/quizzes/{id}` (student token) and crawl-source payloads |
| Route-permission wiring in frontend | `repo/frontend/tests/unit/router/routePermissions.spec.ts:69-214` | Ensures routes include FE permission constants | insufficient | No cross-check with backend canonical permission codes | Add cross-contract test comparing FE constants against backend permission seed/API |

### 8.3 Security Coverage Audit
- Authentication: **Insufficient coverage**
  - Existing tests cover login/invalid creds and basic refresh status (`repo/API_tests/test_auth_endpoints.sh:21-60`) but do not cover refresh token type misuse or logout revocation.
- Route authorization: **Basically covered**
  - Controller tests with role permutations are present (`repo/backend/src/test/java/com/scholarops/controller/SubmissionAuthorizationTest.java:68-188`).
- Object-level authorization: **Insufficient coverage**
  - Some ownership scripts exist (`repo/API_tests/test_submission_ownership.sh:126-143`), but grading-assignee ownership is not meaningfully tested.
- Tenant/data isolation: **Insufficient coverage**
  - Submission ownership gets some test coverage; answer-confidentiality leakage path lacks tests.
- Admin/internal protection: **Basically covered**
  - 401/403 and admin endpoint checks are present (`repo/run_tests.sh:86-109`, `repo/API_tests/test_security_authorization.sh`).

### 8.4 Final Coverage Judgment
- **Partial Pass**
- Covered major risks:
  - baseline authn/authz status checks and several role-boundary checks; documented test entry points and API scripts for 401/403/ownership paths.
- Uncovered or under-tested risks (should be prioritized; not a full **Pass** on coverage):
  - refresh/logout token lifecycle security,
  - student answer confidentiality (`correctAnswer` exposure),
  - service-layer grading object authorization,
  - frontend/backend contract drift for core quiz/grading workflows.

## 9. Final Notes
- Findings were restricted to static evidence only; no runtime behavior was assumed.
- **Partial Pass** implies **targeted remediation** of Blocker/High items and contract/placeholder gaps before treating the system as production-ready; it does **not** require a full rewrite of the stack.
- Any item dependent on runtime behavior remains **Manual Verification Required** after fixes.
