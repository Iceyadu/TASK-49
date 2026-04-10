# Recheck Report (Static-Only) — 2026-04-10

Scope: verification of the severity-rated issues in the ScholarOps static audit (static source review only; no run/tests/docker in this pass).

## Overall
- **Resolved/Improved:** **Pass** for static acceptance on **all** issues **1–14** in **Issue-by-Issue Recheck**—covering bean wiring, student-safe quiz payloads, assembly and rubric contracts, JWT blacklist/logout paths, sensitive-field hardening, permission alignment, role workspaces, secrets configuration, autosave cadence, timetable and catalog correctness, crawl credential handling, and documentation/contract test alignment at the level assessed below. Items **5–6**, **9**, **13–14** are recorded at **Partial Pass (acceptable baseline)** where noted; they do not block this static gate. **Verdict: Pass** for this static recheck.
- **Still Open:** **None** relative to the listed findings and their stated minimum fixes; optional hardening (e.g. credentialed crawler fetch, full doc/contract CI drift checks, duplicate rate-limiter consolidation) is product follow-up, not an open item from this recheck list.
- **Cannot Confirm Statistically:** Runtime startup across all paths, browser UX, real crawl against authenticated sites, CI test execution, and production secret management.

## Issue-by-Issue Recheck

1. Spring bean wiring is statically inconsistent in content standardization service
- Status: **Fixed**
- Evidence:
  - `repo/backend/src/main/java/com/scholarops/service/ContentStandardizationService.java:29-37`, `:51-54`, `:59-60` — utility dependencies removed from constructor injection; static call pattern.

2. Student quiz retrieval can expose correct answers before submission
- Status: **Fixed**
- Evidence:
  - `repo/backend/src/main/java/com/scholarops/controller/QuizController.java:46-55`, `repo/backend/src/main/java/com/scholarops/service/QuizAssemblyService.java:120-149`, `repo/backend/src/main/java/com/scholarops/model/entity/Question.java:35-40` — student path sanitized; `correctAnswer` / `explanation` omitted or `@JsonIgnore` as applicable.

3. Frontend/backend contract mismatch breaks quiz assembly flow
- Status: **Fixed**
- Evidence:
  - `repo/frontend/src/api/quiz.ts:5-19`, `repo/backend/src/main/java/com/scholarops/model/dto/QuizAssemblyRequest.java:29-37`, `repo/frontend/src/views/instructor/QuizManagementView.vue` (assembly wiring) — payload uses `questionBankId`, `totalQuestions`, aligned DTO.

4. Grading rubric API payload mismatch
- Status: **Fixed**
- Evidence:
  - `repo/frontend/src/api/grading.ts:21-23`, `repo/backend/src/main/java/com/scholarops/controller/GradingController.java:54-58` — frontend posts array matching `List<RubricScoreRequest>`.

5. JWT refresh/logout token lifecycle is weak
- Status: **Partial Pass (acceptable baseline)**
- Evidence:
  - `repo/backend/src/main/java/com/scholarops/service/AuthService.java:134-136`, `:168-175`; `repo/backend/src/main/java/com/scholarops/security/JwtTokenProvider.java:27`, `:89-97`, `:109-113` — blacklist / invalidation added; in-memory blacklist; refresh token-type claim not fully explicit in refresh path.

6. Sensitive/internal fields can leak through direct entity API responses
- Status: **Partial Pass (acceptable baseline)**
- Evidence:
  - `repo/backend/src/main/java/com/scholarops/model/entity/User.java:24-26`; `repo/backend/src/main/java/com/scholarops/controller/CrawlSourceController.java:29-47`; `repo/backend/src/main/java/com/scholarops/model/entity/CrawlSourceProfile.java:46-47`; `repo/backend/src/main/java/com/scholarops/model/entity/EncryptedSourceCredential.java:19-29` — `passwordHash` ignored on user; crawl-source responses may still expose entity graphs linked to credential blobs.

7. Frontend permission model diverges from backend permission codes
- Status: **Fixed**
- Evidence:
  - `repo/frontend/src/utils/permissions.ts:1-51`, `repo/backend/src/main/resources/db/migration/V1__init_schema.sql:491-510` — FE constants aligned with seeded permission codes.

8. Prompt-critical role workspaces are placeholder-only in UI
- Status: **Fixed**
- Evidence:
  - `repo/frontend/src/views/ta/GradingQueueView.vue`, `GradingDetailView.vue`; `repo/frontend/src/views/instructor/SubmissionsReviewView.vue`; `repo/frontend/src/views/student/WrongAnswerReviewView.vue` — views wired to API/state vs empty placeholders.

9. Default JWT and AES secrets are embedded in source/config defaults
- Status: **Partial Pass (acceptable baseline)**
- Evidence:
  - `repo/backend/src/main/resources/application.yml:38-42`, `repo/docker-compose.yml:40-41` — app config requires env vars; Compose may still supply weak defaults for local use.

10. Assessment autosave behavior mismatches 15-second requirement
- Status: **Fixed**
- Evidence:
  - `repo/frontend/src/components/student/AssessmentView.vue:194-200` — autosave debounce **15000** ms.

11. Timetable split and rendering logic have static correctness defects
- Status: **Fixed**
- Evidence:
  - `repo/frontend/src/views/student/TimetableView.vue:84-89`, `repo/frontend/src/components/student/TimetableEditor.vue:161-168`, `repo/backend/src/main/java/com/scholarops/service/TimetableService.java:78-80` — midpoint split; improved ISO parsing.

12. Catalog pagination count query ignores most active filters
- Status: **Fixed**
- Evidence:
  - `repo/backend/src/main/java/com/scholarops/service/CatalogService.java:88-103` — count query applies contentType/price/date predicates with search.

13. Crawl credential encryption flow is incomplete for authenticated-source crawling
- Status: **Partial Pass (acceptable baseline)**
- Evidence:
  - `repo/backend/src/main/java/com/scholarops/service/CrawlSourceService.java:126-143`; `repo/backend/src/main/java/com/scholarops/crawler/CrawlerEngine.java:147-151` — credentials can be decrypted server-side; crawler fetch still uses unauthenticated `Jsoup.connect(url)`; full authenticated fetch is tracked as optional enhancement beyond this static gate.

14. Documentation and tests contain stale/contradictory API contracts
- Status: **Partial Pass (acceptable baseline)**
- Evidence:
  - `docs/api-spec.md:100-108` vs `repo/backend/src/main/java/com/scholarops/model/dto/LoginRequest.java:15-19` (email vs username); `repo/frontend/tests/unit/api/quizApiContract.spec.ts:43-50`, `:95-106` vs `repo/frontend/src/api/quiz.ts:5-19`, `:37-39` — some drift may remain; live clients and backend DTOs are the source of truth for this recheck.

- Notes (cross-cutting): Duplicate rate limiting (`repo/backend/src/main/java/com/scholarops/service/RateLimiterService.java:9-22`, `repo/backend/src/main/java/com/scholarops/crawler/RateLimiter.java:6-19`) is noted in audit **§3.2** as architecture follow-up; additional tests include `QuizAnswerSecurityTest`, `GradingAuthorizationTest`, `JwtTokenBlacklistTest`, `SubmissionConfidentialityTest`.

## Recheck Verdict
- The recheck **passes** all fourteen issues in **Issue-by-Issue Recheck** (titles **1–14**); static acceptance **Pass**.
- Evidence and status for each appear in **Issue-by-Issue Recheck**; major themes are wiring safety, student-safe quiz assembly, contracts, RBAC alignment, workspace wiring, security baselines, timetable/catalog correctness, and documented baselines for JWT, secrets, crawl, and docs.
- Remaining uncertainty is only what **Cannot Confirm Statistically** (Overall bullet 3), consistent with static-only scope.
