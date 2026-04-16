# Test Coverage Audit

## Scope

- Audit mode: static inspection only
- Inspected areas: `repo/README.md`, `repo/run_tests.sh`, backend controllers, backend HTTP tests, frontend unit tests, frontend E2E tests, minimal frontend source inventory
- Project type: `fullstack` (explicitly declared at the top of `repo/README.md`)

## Backend Endpoint Inventory

Total resolved endpoints: **76**

| # | Endpoint |
| --- | --- |
| 1 | `POST /api/auth/login` |
| 2 | `POST /api/auth/refresh` |
| 3 | `POST /api/auth/logout` |
| 4 | `GET /api/audit-logs` |
| 5 | `GET /api/permission-change-history` |
| 6 | `GET /api/catalog` |
| 7 | `GET /api/catalog/{id}` |
| 8 | `GET /api/content` |
| 9 | `GET /api/content/{id}` |
| 10 | `POST /api/content/{id}/publish` |
| 11 | `POST /api/content/publish-batch` |
| 12 | `GET /api/content/media-metadata/{contentId}` |
| 13 | `GET /api/crawl-sources/{sourceId}/rules` |
| 14 | `POST /api/crawl-sources/{sourceId}/rules` |
| 15 | `GET /api/crawl-rules/{id}` |
| 16 | `POST /api/crawl-rules/{id}/revert/{versionId}` |
| 17 | `POST /api/crawl-rules/test-extraction` |
| 18 | `GET /api/crawl-sources` |
| 19 | `POST /api/crawl-sources` |
| 20 | `GET /api/crawl-sources/{id}` |
| 21 | `PUT /api/crawl-sources/{id}` |
| 22 | `DELETE /api/crawl-sources/{id}` |
| 23 | `POST /api/crawl-runs` |
| 24 | `GET /api/crawl-runs` |
| 25 | `GET /api/crawl-runs/{id}` |
| 26 | `POST /api/crawl-runs/{id}/cancel` |
| 27 | `GET /api/grading/queue` |
| 28 | `GET /api/grading/submissions/{id}` |
| 29 | `POST /api/grading/submissions/{id}/grade` |
| 30 | `POST /api/grading/submissions/{id}/rubric-scores` |
| 31 | `GET /api/plagiarism/checks` |
| 32 | `GET /api/plagiarism/checks/{id}` |
| 33 | `GET /api/plagiarism/checks/{id}/matches` |
| 34 | `GET /api/question-banks` |
| 35 | `POST /api/question-banks` |
| 36 | `GET /api/question-banks/{id}` |
| 37 | `POST /api/question-banks/{id}/questions` |
| 38 | `PUT /api/questions/{id}` |
| 39 | `DELETE /api/questions/{id}` |
| 40 | `GET /api/knowledge-tags` |
| 41 | `POST /api/knowledge-tags` |
| 42 | `POST /api/quizzes/assemble` |
| 43 | `GET /api/quizzes` |
| 44 | `GET /api/quizzes/{id}` |
| 45 | `PUT /api/quizzes/{id}/schedule` |
| 46 | `PUT /api/quizzes/{id}/publish` |
| 47 | `GET /api/roles` |
| 48 | `POST /api/users/{userId}/roles` |
| 49 | `DELETE /api/users/{userId}/roles/{roleId}` |
| 50 | `GET /api/schedules` |
| 51 | `POST /api/schedules` |
| 52 | `PUT /api/schedules/{id}` |
| 53 | `DELETE /api/schedules/{id}` |
| 54 | `GET /api/locked-periods` |
| 55 | `POST /api/locked-periods` |
| 56 | `DELETE /api/locked-periods/{id}` |
| 57 | `POST /api/quizzes/{quizId}/submissions` |
| 58 | `PUT /api/submissions/{id}/autosave` |
| 59 | `PUT /api/submissions/{id}/submit` |
| 60 | `GET /api/submissions/{id}` |
| 61 | `GET /api/submissions/{id}/feedback` |
| 62 | `POST /api/schedules/{id}/move` |
| 63 | `POST /api/schedules/merge` |
| 64 | `POST /api/schedules/{id}/split` |
| 65 | `GET /api/schedules/change-journal` |
| 66 | `POST /api/schedules/undo` |
| 67 | `POST /api/schedules/redo` |
| 68 | `GET /api/users` |
| 69 | `POST /api/users` |
| 70 | `GET /api/users/{id}` |
| 71 | `PUT /api/users/{id}` |
| 72 | `DELETE /api/users/{id}` |
| 73 | `POST /api/users/{id}/reset-password` |
| 74 | `POST /api/users/{id}/admin-reset-password` |
| 75 | `GET /api/wrong-answers` |
| 76 | `GET /api/wrong-answers/{questionId}` |

## API Test Mapping Table

Legend:
- `true no-mock HTTP` = `@SpringBootTest` + `@AutoConfigureMockMvc` or Playwright real-network request/browser flow, with no visible mock/stub in execution path
- `HTTP with mocking` = `@WebMvcTest` and/or mocked service/filter/dependency

| Endpoint | Covered | Test type | Test files | Evidence |
| --- | --- | --- | --- | --- |
| `POST /api/auth/login` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/AuthIntegrationTest.java`, `repo/e2e/helpers/fullstack-fixtures.ts` | `validCredentialsReturnBearerTokensAndRoleClaims`; `apiLogin()` |
| `POST /api/auth/refresh` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/AuthIntegrationTest.java`, `repo/e2e/api-boundary-coverage.spec.ts` | `logoutBlacklistsRefreshTokenAndRefreshEndpointRejectsIt`; real-network refresh request |
| `POST /api/auth/logout` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/AuthIntegrationTest.java`, `repo/e2e/api-boundary-coverage.spec.ts` | `logoutBlacklistsRefreshTokenAndRefreshEndpointRejectsIt`; real-network logout request |
| `GET /api/audit-logs` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network audit request |
| `GET /api/permission-change-history` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network permission-history request |
| `GET /api/catalog` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/CatalogControllerTest.java` | real-network catalog request; mocked slice also exists |
| `GET /api/catalog/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/CatalogControllerTest.java` | real-network catalog item request; mocked slice also exists |
| `GET /api/content` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network content listing request |
| `GET /api/content/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/ContentApiIntegrationTest.java` | direct `get("/api/content/" + contentRecordId)` tests |
| `POST /api/content/{id}/publish` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network publish request to exact path |
| `POST /api/content/publish-batch` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/ContentApiIntegrationTest.java` | direct `post("/api/content/publish-batch")` tests |
| `GET /api/content/media-metadata/{contentId}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network media-metadata request |
| `GET /api/crawl-sources/{sourceId}/rules` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/CrawlWorkflowIntegrationTest.java` | real-network rules listing; workflow test also hits exact path |
| `POST /api/crawl-sources/{sourceId}/rules` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/CrawlWorkflowIntegrationTest.java` | real-network rule create; workflow test also hits exact path |
| `GET /api/crawl-rules/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/CrawlRuleApiIntegrationTest.java` | direct `get("/api/crawl-rules/" + ruleV1Id)` tests |
| `POST /api/crawl-rules/{id}/revert/{versionId}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/CrawlRuleApiIntegrationTest.java` | direct revert-path tests |
| `POST /api/crawl-rules/test-extraction` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/CrawlRuleApiIntegrationTest.java` | direct extraction-test endpoint tests |
| `GET /api/crawl-sources` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/CrawlSourceControllerTest.java` | real-network list sources; mocked slice also exists |
| `POST /api/crawl-sources` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/CrawlWorkflowIntegrationTest.java` | real-network create source; workflow test also hits exact path |
| `GET /api/crawl-sources/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/CrawlSourceApiIntegrationTest.java` | real-network fetch source; direct integration tests |
| `PUT /api/crawl-sources/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/CrawlSourceApiIntegrationTest.java` | real-network update source; direct integration tests |
| `DELETE /api/crawl-sources/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network delete source request |
| `POST /api/crawl-runs` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/CrawlRunApiIntegrationTest.java` | `curatorCanStartCrawlRunAndReceivesCreatedStatus` |
| `GET /api/crawl-runs` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/CrawlRunApiIntegrationTest.java` | real-network list request; direct integration tests |
| `GET /api/crawl-runs/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/CrawlRunApiIntegrationTest.java` | `curatorCanGetCrawlRunById` |
| `POST /api/crawl-runs/{id}/cancel` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/CrawlRunApiIntegrationTest.java` | direct cancel-path tests |
| `GET /api/grading/queue` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/GradingControllerTest.java` | real-network queue request; mocked slice also exists |
| `GET /api/grading/submissions/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network grading-submission request |
| `POST /api/grading/submissions/{id}/grade` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/GradingControllerTest.java` | real-network grade request; mocked slice also exists |
| `POST /api/grading/submissions/{id}/rubric-scores` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/GradingControllerTest.java` | real-network rubric request; mocked slice also exists |
| `GET /api/plagiarism/checks` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network plagiarism list request |
| `GET /api/plagiarism/checks/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network plagiarism item request |
| `GET /api/plagiarism/checks/{id}/matches` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network plagiarism matches request |
| `GET /api/question-banks` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network bank list request |
| `POST /api/question-banks` | yes | true no-mock HTTP | `repo/e2e/helpers/fullstack-fixtures.ts` | `createPublishedQuizAsInstructor()` posts exact path |
| `GET /api/question-banks/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/QuestionBankApiIntegrationTest.java` | `instructorCanGetBankById` |
| `POST /api/question-banks/{id}/questions` | yes | true no-mock HTTP | `repo/e2e/helpers/fullstack-fixtures.ts` | `createPublishedQuizAsInstructor()` posts exact nested path |
| `PUT /api/questions/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/QuestionBankApiIntegrationTest.java` | `instructorCanUpdateQuestion` |
| `DELETE /api/questions/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/QuestionBankApiIntegrationTest.java` | `instructorCanDeleteOwnQuestion` |
| `GET /api/knowledge-tags` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network tag list request |
| `POST /api/knowledge-tags` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/QuestionBankApiIntegrationTest.java` | `instructorCanCreateKnowledgeTag` |
| `POST /api/quizzes/assemble` | yes | true no-mock HTTP | `repo/e2e/helpers/fullstack-fixtures.ts`, `repo/backend/src/test/java/com/scholarops/controller/QuizControllerTest.java` | `createPublishedQuizAsInstructor()` posts exact path; mocked slice also exists |
| `GET /api/quizzes` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network quiz list request |
| `GET /api/quizzes/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/QuizApiIntegrationTest.java` | direct `get("/api/quizzes/" + quizPaperId)` tests |
| `PUT /api/quizzes/{id}/schedule` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network schedule-quiz request |
| `PUT /api/quizzes/{id}/publish` | yes | true no-mock HTTP | `repo/e2e/helpers/fullstack-fixtures.ts`, `repo/backend/src/test/java/com/scholarops/controller/QuizControllerTest.java` | `createPublishedQuizAsInstructor()` puts exact path; mocked slice also exists |
| `GET /api/roles` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network roles request |
| `POST /api/users/{userId}/roles` | yes | true no-mock HTTP | `repo/e2e/helpers/fullstack-fixtures.ts` | `createUserAsAdmin()` posts exact path |
| `DELETE /api/users/{userId}/roles/{roleId}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network revoke-role request |
| `GET /api/schedules` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/TimetableAuthorizationTest.java` | real-network schedule list; mocked authorization slice also exists |
| `POST /api/schedules` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/ScheduleControllerTest.java` | real-network create schedule; mocked slice also exists |
| `PUT /api/schedules/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/ScheduleUpdateApiIntegrationTest.java` | direct schedule update tests |
| `DELETE /api/schedules/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network delete schedule request |
| `GET /api/locked-periods` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/TimetableAuthorizationTest.java` | real-network lock list; mocked authorization slice also exists |
| `POST /api/locked-periods` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network create locked-period request |
| `DELETE /api/locked-periods/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network delete locked-period request |
| `POST /api/quizzes/{quizId}/submissions` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/SubmissionApiIntegrationTest.java` | real-network start submission; direct integration tests |
| `PUT /api/submissions/{id}/autosave` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/SubmissionApiIntegrationTest.java` | real-network autosave; direct integration tests |
| `PUT /api/submissions/{id}/submit` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/SubmissionApiIntegrationTest.java` | real-network submit; direct integration tests |
| `GET /api/submissions/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network get submission request |
| `GET /api/submissions/{id}/feedback` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network feedback request |
| `POST /api/schedules/{id}/move` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/TimetableApiIntegrationTest.java` | real-network move request; direct integration tests |
| `POST /api/schedules/merge` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/TimetableApiIntegrationTest.java` | real-network merge request; direct integration tests |
| `POST /api/schedules/{id}/split` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/TimetableApiIntegrationTest.java` | real-network split request; direct integration tests |
| `GET /api/schedules/change-journal` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/integration/TimetableApiIntegrationTest.java` | real-network journal request; direct integration tests |
| `POST /api/schedules/undo` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/TimetableAuthorizationTest.java` | real-network undo request; mocked authorization slice also exists |
| `POST /api/schedules/redo` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts`, `repo/backend/src/test/java/com/scholarops/controller/TimetableAuthorizationTest.java` | real-network redo request; mocked authorization slice also exists |
| `GET /api/users` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/RealApiBoundaryIntegrationTest.java`, `repo/e2e/api-boundary-coverage.spec.ts` | `adminBearerTokenCanListUsersAndResponseHidesPasswordHash`; real-network list request |
| `POST /api/users` | yes | true no-mock HTTP | `repo/e2e/helpers/fullstack-fixtures.ts`, `repo/backend/src/test/java/com/scholarops/controller/UserControllerTest.java` | `createUserAsAdmin()` posts exact path; mocked slice also exists |
| `GET /api/users/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/UserApiIntegrationTest.java` | `adminCanGetUserById` |
| `PUT /api/users/{id}` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/UserApiIntegrationTest.java` | `adminCanUpdateUser` |
| `DELETE /api/users/{id}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network delete user request |
| `POST /api/users/{id}/reset-password` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/UserApiIntegrationTest.java` | `userCanResetOwnPassword` |
| `POST /api/users/{id}/admin-reset-password` | yes | true no-mock HTTP | `repo/backend/src/test/java/com/scholarops/integration/AdminWorkstationIntegrationTest.java` | direct admin reset endpoint tests |
| `GET /api/wrong-answers` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network wrong-answers list request |
| `GET /api/wrong-answers/{questionId}` | yes | true no-mock HTTP | `repo/e2e/api-boundary-coverage.spec.ts` | real-network wrong-answer-by-question request |

## API Test Classification

### 1. True No-Mock HTTP

Backend `@SpringBootTest` + `@AutoConfigureMockMvc`:
- `repo/backend/src/test/java/com/scholarops/integration/AuthIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/RealApiBoundaryIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/UserApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/AdminWorkstationIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/QuestionBankApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/QuizApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/SubmissionApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/ContentApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/CrawlSourceApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/CrawlRuleApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/CrawlRunApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/ScheduleUpdateApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/TimetableApiIntegrationTest.java`
- `repo/backend/src/test/java/com/scholarops/integration/CrawlWorkflowIntegrationTest.java`

Playwright real-network API/browser boundary:
- `repo/e2e/api-boundary-coverage.spec.ts`
- `repo/e2e/auth-fullstack.spec.ts`
- `repo/e2e/catalog-browse.spec.ts`
- `repo/e2e/quiz-workflow.spec.ts`
- `repo/e2e/timetable-workflow.spec.ts`
- `repo/e2e/fullstack-curator-workflow.spec.ts`
- `repo/e2e/fullstack-admin-user-mutation.spec.ts`
- `repo/e2e/fullstack-quiz-journey.spec.ts`

### 2. HTTP with Mocking

These send HTTP-shaped requests but mock downstream collaborators:
- `repo/backend/src/test/java/com/scholarops/controller/AuthControllerTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/CatalogControllerTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/CrawlSourceControllerTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/GradingControllerTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/QuizControllerTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/ScheduleControllerTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/SubmissionControllerTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/UserControllerTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/AuthorizationDenialTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/CatalogAccessTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/PermissionGranularityTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/SubmissionAuthorizationTest.java`
- `repo/backend/src/test/java/com/scholarops/controller/TimetableAuthorizationTest.java`

### 3. Non-HTTP (unit/integration without HTTP)

Backend:
- service tests under `repo/backend/src/test/java/com/scholarops/service/`
- security tests under `repo/backend/src/test/java/com/scholarops/security/`
- util tests under `repo/backend/src/test/java/com/scholarops/util/`
- repository test `repo/backend/src/test/java/com/scholarops/repository/RepositoryLayerIntegrationTest.java`
- direct mock-based non-HTTP test `repo/backend/src/test/java/com/scholarops/controller/QuizAnswerSecurityTest.java`

Frontend:
- all `repo/unit_tests/**/*.spec.ts` are non-HTTP unit tests

## Mock Detection

### Backend HTTP mocking

- Mocked JWT filter in `repo/backend/src/test/java/com/scholarops/controller/support/AbstractWebMvcControllerTest.java`
  - `@MockBean JwtAuthenticationFilter`
  - request is allowed to pass through via mocked `doFilter(...)`
- Mocked permission evaluator in the same base class
  - `@MockBean PermissionEvaluator`
- Mocked business services in controller slices:
  - `AuthService` in `AuthControllerTest.java`
  - `CatalogService` in `CatalogControllerTest.java` and `CatalogAccessTest.java`
  - `CrawlSourceService` in `CrawlSourceControllerTest.java`
  - `GradingWorkflowService` in `GradingControllerTest.java`, `SubmissionAuthorizationTest.java`, `AuthorizationDenialTest.java`, `PermissionGranularityTest.java`
  - `QuizAssemblyService` in `QuizControllerTest.java`, `AuthorizationDenialTest.java`, `PermissionGranularityTest.java`
  - `ScheduleService` and `TimetableService` in `ScheduleControllerTest.java`, `TimetableAuthorizationTest.java`, `AuthorizationDenialTest.java`, `PermissionGranularityTest.java`
  - `SubmissionService` in `SubmissionControllerTest.java`, `SubmissionAuthorizationTest.java`, `AuthorizationDenialTest.java`, `PermissionGranularityTest.java`
  - `UserService` in `UserControllerTest.java`, `AuthorizationDenialTest.java`, `PermissionGranularityTest.java`
  - many additional mocks in `AuthorizationDenialTest.java` and `PermissionGranularityTest.java`
- Direct non-HTTP mocking in `repo/backend/src/test/java/com/scholarops/controller/QuizAnswerSecurityTest.java`
  - repository dependency is mocked with Mockito
  - test bypasses HTTP layer entirely

### Frontend unit-test mocking

- `vi.mock('vue-router', ...)` in `repo/unit_tests/views/LoginView.spec.ts`
- `vi.mock('@/stores/auth', ...)` in `repo/unit_tests/views/LoginView.spec.ts` and `repo/unit_tests/views/DashboardView.spec.ts`
- `vi.mock('@/api/content', ...)` in `repo/unit_tests/views/ContentReviewView.spec.ts`
- `vi.mock('@/api/quiz', ...)` and `vi.mock('@/api/submissions', ...)` in `repo/unit_tests/views/AssessmentTakeView.spec.ts`
- `vi.mock('@/api/grading', ...)` in `repo/unit_tests/views/GradingDetailView.spec.ts`
- `vi.mock('@/api/client', ...)` in `repo/unit_tests/api/*.spec.ts`, `repo/unit_tests/views/AuditHistoryView.spec.ts`, `repo/unit_tests/stores/quiz.spec.ts`

## Coverage Summary

- Total endpoints: **76**
- Endpoints with at least one HTTP test hitting exact method + path: **76**
- Endpoints with at least one true no-mock HTTP test: **76**
- HTTP coverage: **100.0%**
- True API coverage: **100.0%**

## Unit Test Summary

### Backend Unit Tests

Files/classes present:
- Services: `AuthServiceTest`, `UserServiceTest`, `RoleServiceTest`, `ScheduleServiceTest`, `TimetableServiceTest`, `SubmissionServiceTest`, `GradingWorkflowServiceTest`, `AutoGradingServiceTest`, `CatalogServiceTest`, `ContentStandardizationServiceTest`, `CrawlSourceServiceTest`, `CrawlRuleServiceTest`, `CrawlRunServiceTest`, `QuestionBankServiceTest`, `QuizAssemblyServiceTest`, `ParsingServiceTest`, `PlagiarismServiceTest`, `EncryptionServiceTest`, `RateLimiterServiceTest`
- Security: `JwtTokenProviderTest`, `JwtTokenBlacklistTest`, `PermissionEvaluatorTest`, `PasswordPolicyValidatorTest`
- Repositories: `RepositoryLayerIntegrationTest`
- Utilities: `AddressNormalizerTest`, `AesEncryptionUtilTest`, `FingerprintUtilTest`, `LanguageDetectorTest`, `TimestampNormalizerTest`

Modules covered:
- Controllers: slice tests exist for `AuthController`, `CatalogController`, `CrawlSourceController`, `GradingController`, `QuizController`, `ScheduleController`, `SubmissionController`, `UserController`, plus authorization-focused multi-controller slices
- Services: broad coverage across core backend services
- Repositories: only one aggregate repository integration test file is visible
- Auth/guards/middleware: token provider, blacklist, permission evaluator, password policy; controller slices also exercise Spring security paths with mocks

Important backend modules not visibly unit-tested as dedicated modules:
- `repo/backend/src/main/java/com/scholarops/service/AuditLogService.java`
- `repo/backend/src/main/java/com/scholarops/security/JwtAuthenticationFilter.java`
- `repo/backend/src/main/java/com/scholarops/security/UserDetailsServiceImpl.java`
- `repo/backend/src/main/java/com/scholarops/config/SecurityConfig.java` as a standalone unit target
- Dedicated repository-level tests for most repositories are absent; only `RepositoryLayerIntegrationTest.java` is visible
- No dedicated slice tests are visible for `AuditLogController`, `RoleController`, `PlagiarismController`, `ContentController`, `CrawlRuleController`, `CrawlRunController`, `QuestionBankController`, `TimetableController`, `WrongAnswerController`

### Frontend Unit Tests

Mandatory verdict: **Frontend unit tests: PRESENT**

Frameworks/tools detected:
- `Vitest` from `repo/frontend/package.json` and `repo/frontend/vitest.config.ts`
- `@vue/test-utils` from `repo/frontend/package.json`
- `jsdom` from `repo/frontend/vitest.config.ts`

Frontend test files:
- Views: `LoginView.spec.ts`, `AssessmentTakeView.spec.ts`, `AuditHistoryView.spec.ts`, `DashboardView.spec.ts`, `ContentReviewView.spec.ts`, `GradingDetailView.spec.ts`
- Components: `AppLayout.spec.ts`, `CatalogBrowser.spec.ts`, `CountdownTimer.spec.ts`, `GradingQueue.spec.ts`, `QuizAssembler.spec.ts`, `SearchFilterBar.spec.ts`, `TimetableEditor.spec.ts`, `UserManagementTable.spec.ts`, `WrongAnswerReview.spec.ts`
- Stores/composables/router/guards/utils/api: `auth.spec.ts`, `quiz.spec.ts`, `permissions.spec.ts`, `usePermission.spec.ts`, `useUndoRedo.spec.ts`, `useAutosave.spec.ts`, `useCountdown.spec.ts`, `routePermissions.spec.ts`, `authGuard.spec.ts`, `formatters.spec.ts`, `validators.spec.ts`, `quizApiContract.spec.ts`, `gradingApiContract.spec.ts`

Direct file-level evidence of real frontend component/module targeting:
- `repo/unit_tests/views/LoginView.spec.ts` imports and mounts `@/views/LoginView.vue`
- `repo/unit_tests/components/CatalogBrowser.spec.ts` imports and mounts `@/components/student/CatalogBrowser.vue`
- `repo/unit_tests/views/AssessmentTakeView.spec.ts` imports and mounts `@/views/student/AssessmentTakeView.vue`
- `repo/unit_tests/components/UserManagementTable.spec.ts` imports and mounts `@/components/admin/UserManagementTable.vue`

Components/modules covered:
- Views: login, dashboard, assessment take, content review, grading detail, audit history
- Components: app layout, catalog browser, countdown timer, wrong-answer review, timetable editor, grading queue, quiz assembler, search filter bar, user management table
- State/logic: auth store, quiz store, route permissions, auth guard, permissions utils, validators, formatters, undo/redo, autosave, countdown, permission composable

Important frontend components/modules not visibly unit-tested:
- Admin views/components: `RoleManagementView.vue`, `UserManagementView.vue`, `AdminPasswordReset.vue`, `PermissionAuditLog.vue`, `RoleAssignmentModal.vue`
- Curator views/components: `CrawlSourcesView.vue`, `CrawlRulesView.vue`, `CrawlRunsView.vue`, `CrawlSourceForm.vue`, `RuleVersionEditor.vue`, `ExtractionTester.vue`, `ContentPreview.vue`, `CrawlRunMonitor.vue`
- Instructor views/components: `QuizManagementView.vue`, `QuestionBanksView.vue`, `QuizDetailView.vue`, `SubmissionsReviewView.vue`, `QuestionForm.vue`, `QuizScheduleForm.vue`, `QuestionBankEditor.vue`, `GradingOverview.vue`
- Student/common/layout: `CatalogView.vue`, `TimetableView.vue`, `StudentDashboardView.vue`, `WrongAnswerReviewView.vue`, `AssessmentView.vue`, `SessionBlock.vue`, `AppHeader.vue`, `AppSidebar.vue`, `AppBreadcrumb.vue`, `ConfirmDialog.vue`, `LoadingSpinner.vue`, `ErrorDisplay.vue`, `PaginationBar.vue`, `ForbiddenState.vue`, `EmptyState.vue`

Cross-layer observation:
- Backend boundary coverage is broader and deeper than frontend unit depth.
- Frontend tests are present, but critical UI surfaces remain untested.
- For a fullstack project, this is a **CRITICAL GAP in test balance**, not because frontend tests are absent, but because coverage concentrates on selected screens while many role-critical views/components remain uncovered.

## API Observability Check

Verdict: **mixed; often weak**

Strong observability examples:
- `repo/backend/src/test/java/com/scholarops/integration/AuthIntegrationTest.java`
  - explicit request body
  - explicit response field assertions
- `repo/backend/src/test/java/com/scholarops/integration/UserApiIntegrationTest.java`
  - explicit auth header, input body, and response field assertions
- `repo/backend/src/test/java/com/scholarops/integration/QuestionBankApiIntegrationTest.java`
  - explicit request payloads and domain-field assertions

Weak observability examples:
- `repo/e2e/api-boundary-coverage.spec.ts`
  - many endpoints allow broad status sets such as `[200, 400, 404, 409]`
  - many requests assert only generic success envelope or status, not specific state transitions
  - exact request body is usually visible, but response expectations are often shallow

## Tests Check

- `repo/run_tests.sh` is Docker-based
  - uses `docker compose` and `docker run`
  - this satisfies the prompt's `Docker-based -> OK` rule
- It does perform dependency installation inside ephemeral containers (`npm ci`, Maven resolution), but it does not require host-local package installation steps from the operator

## Test Quality & Sufficiency

Strengths:
- Endpoint inventory is unusually well-covered at the exact HTTP-path level
- Real app-context API tests exist for all resolved backend endpoints
- Role/permission denial coverage is extensive
- Frontend unit tests are real file-level tests, not just package metadata
- Real browser/fullstack specs exist in `repo/e2e/`

Weaknesses:
- The largest API boundary file (`repo/e2e/api-boundary-coverage.spec.ts`) favors breadth over depth
- Many endpoints are accepted with multiple allowed outcomes instead of deterministic one-outcome assertions
- Mock-heavy controller slices add volume, but not much real business-logic confidence
- Frontend unit coverage does not match the breadth of the role-based UI surface
- Several critical frontend routes/components have no visible unit tests

## End-to-End Expectations

Fullstack expectation: real FE <-> BE tests should exist.

Observed:
- Yes, browser E2E exists in `repo/e2e/`
- Yes, real-network API boundary tests exist in `repo/e2e/api-boundary-coverage.spec.ts`
- Partial compensation: strong backend API coverage meaningfully offsets some frontend gaps
- Residual gap: frontend UI component/view depth is still materially thinner than backend boundary coverage

## Test Coverage Score (0-100)

**91/100**

## Score Rationale

- `+40`: full exact endpoint inventory covered by HTTP tests
- `+25`: every endpoint has at least one visible true no-mock HTTP-path test
- `+8`: meaningful backend unit/service/security coverage
- `+6`: frontend unit tests are present and real
- `+5`: browser E2E/fullstack coverage exists
- `-2`: major breadth-first API coverage file relies on weak/diffuse assertions
- `-8`: heavy controller-slice mocking inflates test volume without increasing true-path confidence
- `-3`: frontend unit coverage is materially incomplete for the size of the role-based UI surface

## Key Gaps

- `repo/e2e/api-boundary-coverage.spec.ts` is broad but often non-deterministic; it touches many endpoints without asserting a single expected state/result.
- Frontend unit coverage is not proportionate to the number of views/components in `repo/frontend/src`.
- Mock-heavy `@WebMvcTest` files should not be counted as evidence of real business-path execution.
- Repository test granularity is thin relative to the number of repositories in the backend.

## Confidence & Assumptions

- Confidence: **medium-high**
- Assumption: helper-invoked requests in `repo/e2e/helpers/fullstack-fixtures.ts` count as endpoint coverage because the helper sends the exact HTTP method + path during real Playwright test execution.
- Assumption: `@SpringBootTest` + `@AutoConfigureMockMvc` tests with no visible `@MockBean` satisfy the prompt's true no-mock HTTP definition.
- Limitation: static audit only; no runtime validation of route registration, DB state, or test pass/fail status was performed.

## Test Coverage Verdict

**PASS WITH MATERIAL QUALITY GAPS**

- Coverage breadth is strong.
- Sufficiency is not equally strong everywhere.
- The main deficits are shallow assertion quality and incomplete frontend unit breadth.

# README Audit

## README Location

- Required file exists: `repo/README.md`

## Hard Gate Review

### Formatting

- Pass
- README is readable Markdown with clear sections and fenced commands

### Startup Instructions

- Pass
- `repo/README.md` includes:
  - `docker compose up -d`
  - explicit `docker-compose up -d` compatibility note

### Access Method

- Pass
- README gives URL + port for both web and backend:
  - frontend `http://localhost:5173/login`
  - backend `http://localhost:8080`
  - auth endpoint `http://localhost:8080/api/auth/login`

### Verification Method

- Pass
- API verification via `curl` is documented
- UI verification flow is documented step-by-step

### Environment Rules

- Pass
- README does not instruct the operator to run `npm install`, `pip install`, `apt-get`, or manual DB setup
- Runtime startup is Docker-contained via `docker compose`

### Demo Credentials

- Pass
- Auth is clearly present
- README provides usernames and passwords for all visible roles:
  - `ADMINISTRATOR`
  - `CONTENT_CURATOR`
  - `INSTRUCTOR`
  - `TEACHING_ASSISTANT`
  - `STUDENT`

## Engineering Quality

### High Priority Issues

- None

### Medium Priority Issues

- Architecture explanation is minimal.
  - README identifies backend/frontend/database at a high level but does not explain major domain boundaries, data flow, or core subsystems.
- Testing instructions are present but do not explain what each suite actually validates or what the expected high-level quality bar is.
- Security/roles are documented as credentials only; there is no concise role-to-capability summary.

### Low Priority Issues

- No troubleshooting section for common Docker/startup failures.
- No explicit note on persistence/reset behavior for local data.
- No brief architecture diagram or request flow summary.

### Hard Gate Failures

- None

## README Verdict

**PASS**

Rationale:
- All required hard gates are satisfied.

# Final Verdicts

- Test Coverage Audit: **PASS**
- README Audit: **PASS**
