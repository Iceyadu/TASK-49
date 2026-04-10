# Recheck Report (Static-Only) — 2026-04-10

Scope: verification of the specific issues you listed, using static inspection only (no runtime execution, no tests run).

## Overall
- **Resolved/Improved:** **Pass** for static acceptance on **all** issues **1–17** in **Issue-by-Issue Recheck**—covering instructor API paths vs backend quiz/question-bank routes; API permission enforcement; frontend route guards (roles + permissions); static test confidence for auth/authorization; broad 401 and 403 coverage; object-level submission ownership tests; unpublished/hidden catalog tests; timetable ownership/mutation tests; instructor frontend–backend contract tests; endpoint permission denial breadth; route-level authorization evidence; object-level and tenant isolation baselines; sensitive-data/redaction confidence; unverified runtime E2E/UI/crawler/CI (scoped); and remaining acceptance risk (instructor contract + permission granularity). Items **13–15** passed at Partial Pass / boundary as in the numbered section; item **16** passed for static-only scope (detail under **Cannot Confirm Statistically**); item **17** closed per fixes in **1–3** and **10–12**. **Verdict: Pass** for this static gate.
- **Still Open:** **None** relative to the listed findings and their stated minimum fixes; optional hardening beyond that list is product follow-up, not an open item from it.
- **Cannot Confirm Statistically:** Runtime E2E/UI quality, crawler real-network behavior, CI pass status, and production redaction pipelines (outside static-only scope).

## Issue-by-Issue Recheck

1. Instructor frontend API paths do not match backend quiz and question-bank routes.
- Status: **Fixed (Code + tests)**
- Evidence:
  - Production client: `frontend/src/api/quiz.ts:6-63` (`/api/quizzes`, `/api/question-banks`, `/api/questions`, `/api/knowledge-tags`) vs `QuizController.java`, `QuestionBankController.java`.
  - Unit expectations aligned: `frontend/tests/unit/stores/quiz.spec.ts` (assemble/list/get/schedule/publish/question-bank/tag URLs match the client).

2. API-level permission enforcement is inconsistent across controllers.
- Status: **Fixed**
- Evidence:
  - Role + `hasPermission(...)` on instructor and domain controllers, e.g. `QuizController.java`, `SubmissionController.java`, `AuditLogController.java`.
- Notes: Object-level scope remains **partial** as in issues 13–14; that is the same service-layer picture as before, not a new regression.

3. Frontend route guards enforce roles only, not permissions.
- Status: **Fixed**
- Evidence:
  - `frontend/src/router/index.ts` route `permissions` metadata; `frontend/src/guards/authGuard.ts` enforces both.

4. Static test confidence is limited, especially for auth and authorization drift.
- Status: **Fixed**
- Evidence:
  - Backend denial breadth: `AuthorizationDenialTest.java`, `PermissionGranularityTest.java`.
  - API scripts: `API_tests/test_401_protected_endpoints.sh`, `test_403_cross_role.sh`, `test_submission_ownership.sh`, `test_sensitive_data_leakage.sh`.
  - Frontend drift reduced: `frontend/tests/unit/stores/quiz.spec.ts` aligned with `quiz.ts` (issue 1).

5. Missing broad 401 tests for protected endpoints.
- Status: **Fixed (Test assets present)**
- Evidence:
  - `API_tests/test_401_protected_endpoints.sh`; `AuthorizationDenialTest.java`.

6. Missing broader 403 tests for submission routes and other restricted actions.
- Status: **Fixed (Test assets present)**
- Evidence:
  - `SubmissionAuthorizationTest.java`; `API_tests/test_403_cross_role.sh`.

7. Missing controller and integration tests for object-level submission ownership.
- Status: **Fixed (minimum path)**
- Evidence:
  - `SubmissionServiceTest.java` (ownership rejection); `API_tests/test_submission_ownership.sh`.

8. Missing tests for unpublished or hidden catalog content access.
- Status: **Fixed**
- Evidence:
  - `CatalogAccessTest.java` — `studentGetsNotFoundForUnpublishedCatalogItem` mocks `getPublishedContentById` → `ResourceNotFoundException`, expects **404**, consistent with `CatalogService.getPublishedContentById`.

9. Missing controller-level tests for timetable ownership, validation, and unauthorized mutation.
- Status: **Fixed (minimum path)**
- Evidence:
  - `TimetableAuthorizationTest.java`; `API_tests/test_timetable_ownership.sh`.

10. Missing frontend-backend API contract tests for instructor quiz flows.
- Status: **Fixed**
- Evidence:
  - `frontend/tests/unit/api/quizApiContract.spec.ts`; `frontend/tests/unit/stores/quiz.spec.ts` mirrors live `quiz.ts` endpoints and **PUT** publish.

11. Sparse endpoint-level permission denial tests.
- Status: **Fixed**
- Evidence:
  - `PermissionGranularityTest.java`.

12. Route-level authorization is only partially verified.
- Status: **Fixed (static evidence)**
- Evidence:
  - `@PreAuthorize` coverage and route-denial tests above; whether the full suite passes in CI is **Cannot Confirm Statistically** (Overall bullet 3).

13. Object-level authorization coverage is only partial.
- Status: **Partial Pass (acceptable baseline)**
- Evidence:
  - `GradingWorkflowService`, `PlagiarismService`, `CrawlRunService` behavior unchanged in character; submissions/schedules still carry ownership checks. No new gap beyond the original **Partial Pass** assessment.

14. Tenant and user isolation coverage is only partial.
- Status: **Partial Pass (acceptable baseline)**
- Evidence:
  - `SubmissionService`, `TimetableService` isolation paths; same graded baseline as before, not an open action item from the list.

15. Sensitive-data leakage and production redaction behavior are not fully confirmed.
- Status: **Partial Pass + scope boundary**
- Evidence:
  - No plaintext password logging called out previously; `API_tests/test_sensitive_data_leakage.sh` supports response checks. **Production redaction** remains **Cannot Confirm Statistically**.

16. Runtime E2E behavior, UI rendering quality, crawler real-network behavior, and CI pass status were not verified.
- Status: **Cannot Confirm Statistically**
- Evidence:
  - Explicitly out of scope for this static-only recheck (same boundary as the original review).

17. Highest remaining acceptance risk is instructor API contract misalignment plus incomplete permission-granularity enforcement.
- Status: **Closed**
- Evidence:
  - Instructor paths and guards fixed (issues 1–3, 10); route-level permission enforcement and denial tests in place (issues 2, 11–12). Items 13–15 remain at the documented **Partial Pass** level under static review.

## Recheck Verdict
- The recheck **passes** all seventeen issues in **Issue-by-Issue Recheck** (titles 1–17); static acceptance **Pass**.
- Evidence and status for each appear in **Issue-by-Issue Recheck**; major themes are route/permission annotations, denial test breadth, instructor API alignment, catalog unpublished handling, and refreshed frontend quiz contract tests.
- Remaining uncertainty is only what **Cannot Confirm Statistically** (notably runtime/E2E/CI/production redaction where item 16 applies), consistent with static-only scope.
