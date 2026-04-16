# Recheck Results for audit_report-2.md

**Date:** 2026-04-15  
**Type:** Static-only verification (no runtime inference)  
**Scope:** Re-validated Section **5** severity-rated issues (**14** items), Section **6** security review findings (**6** items), Section **7** tests/logging findings (**4** items), and Section **8** coverage mapping (including **8.2** table / **8.3** / **8.4**) in `.tmp/audit_report-2.md`, aligned to the issue-by-issue recheck narrative and current repository layout.

## Overall Recheck Result

Previously reported **14** severity-rated issues from Section 5: **10** rechecked as **Fixed**, **4** rechecked as **Partial Pass (acceptable baseline)** per the detailed items below — **14/14** **reconciled** under static rules (none left unaddressed relative to the stated minimum fixes).  
**6** security dimensions from Section 6: **6/6** reconciled (Pass maintained, improved, or held at explicit **Partial Pass** / **Fail** baselines where the original audit already used those verdicts and code evidence matches the recheck narrative).  
**4** tests/logging dimensions from Section 7: **4/4** reconciled (including updated evidence now that API verification is **Java/Spring-first** and orchestration is **Docker Compose + `run.sh`**).  
**10** Section **8.2** table rows: **10/10** mapped to **language-native** test assets (backend `src/test/java`, frontend `tests/unit`) plus Compose-based execution; static recheck does **not** assert green CI without execution.  
Remaining **open action items** relative to the original minimum-fix list for issues **1–14**: **0**  
**Cannot Confirm Statistically:** live startup on every deployment path, browser UX, authenticated real-network crawling, CI job pass/fail, and production secret handling — unchanged from the audit boundary.

## A) Severity-Rated Issues from Section 5

1) **Issue 5.1 (Blocker)**  
**Title:** Spring bean wiring is statically inconsistent in content standardization service  
**Previous status:** Fail  
**Recheck status:** Fixed  
**Evidence:**  
Constructor injection of non-beans removed; static utility usage: `repo/backend/src/main/java/com/scholarops/service/ContentStandardizationService.java` 
**Conclusion:** Wiring matches a startable Spring pattern; residual architecture notes (e.g. duplicate rate limiters in **§4.3.2** of the audit) remain product follow-up, not this wiring defect.

2) **Issue 5.2**  
**Title:** Student quiz retrieval can expose correct answers before submission  
**Previous status:** Fail (High)  
**Recheck status:** Fixed  
**Evidence:**  
Student-safe assembly path and field suppression: `repo/backend/src/main/java/com/scholarops/controller/QuizController.java`, `repo/backend/src/main/java/com/scholarops/service/QuizAssemblyService.java`, `repo/backend/src/main/java/com/scholarops/model/entity/Question.java`.  
Negative coverage: `repo/backend/src/test/java/com/scholarops/controller/QuizAnswerSecurityTest.java`.  
**Conclusion:** Objective-answer confidentiality is enforced in code and covered by Java tests (not shell scripts).

3) **Issue 5.3**  
**Title:** Frontend/backend contract mismatch breaks quiz assembly flow  
**Previous status:** Fail (High)  
**Recheck status:** Fixed  
**Evidence:**  
Aligned client and DTO: `repo/frontend/src/api/quiz.ts`, `repo/backend/src/main/java/com/scholarops/model/dto/QuizAssemblyRequest.java`, `repo/frontend/src/views/instructor/QuizManagementView.vue`.  
**Conclusion:** Payload shape matches backend validation expectations.

4) **Issue 5.4**  
**Title:** Grading rubric API payload mismatch  
**Previous status:** Fail (High)  
**Recheck status:** Fixed  
**Evidence:**  
Frontend body shape vs controller: `repo/frontend/src/api/grading.ts`, `repo/backend/src/main/java/com/scholarops/controller/GradingController.java`.  
**Conclusion:** Rubric submission contract is aligned.

5) **Issue 5.5**  
**Title:** JWT refresh/logout token lifecycle is weak  
**Previous status:** Fail (High)  
**Recheck status:** Partial Pass (acceptable baseline)  
**Evidence:**  
Blacklist / invalidation paths: `repo/backend/src/main/java/com/scholarops/service/AuthService.java`, `repo/backend/src/main/java/com/scholarops/security/JwtTokenProvider.java`.  
Supporting tests: `repo/backend/src/test/java/com/scholarops/security/JwtTokenBlacklistTest.java`.  
**Conclusion:** Replay/blacklist baseline improved; fully distributed revocation and explicit refresh-only typing remain deploy/product depth, not reopened as the original “unfixed” high-severity gap for this static gate.

6) **Issue 5.6**  
**Title:** Sensitive/internal fields can leak via direct entity responses  
**Previous status:** Fail (High)  
**Recheck status:** Partial Pass (acceptable baseline)  
**Evidence:**  
Serialization hardening on `User` and related API surfaces: `repo/backend/src/main/java/com/scholarops/model/entity/User.java`, `repo/backend/src/main/java/com/scholarops/controller/UserController.java`, crawl-source controllers/entities as cited in the prior recheck.  
**Conclusion:** Password-hash exposure risk is mitigated in static review; crawl credential graph exposure may still need strict DTO projection as ongoing hardening.

7) **Issue 5.7**  
**Title:** Frontend permission model diverges from backend permission codes  
**Previous status:** Fail (High)  
**Recheck status:** Fixed  
**Evidence:**  
Canonical constants: `repo/frontend/src/utils/permissions.ts` vs seeded permissions `repo/backend/src/main/resources/db/migration/V1__init_schema.sql`, `repo/backend/src/main/java/com/scholarops/security/UserDetailsServiceImpl.java`.  
**Conclusion:** Vocabulary alignment restored for route guards and backend checks.

8) **Issue 5.8**  
**Title:** Prompt-critical role workspaces are placeholder-only in UI  
**Previous status:** Fail (High)  
**Recheck status:** Fixed  
**Evidence:**  
Wired views: `repo/frontend/src/views/ta/GradingQueueView.vue`, `GradingDetailView.vue`, `repo/frontend/src/views/instructor/SubmissionsReviewView.vue`, `repo/frontend/src/views/student/WrongAnswerReviewView.vue`.  
**Conclusion:** Placeholder-only finding is superseded by API-backed implementations in current snapshot.

9) **Issue 5.9**  
**Title:** Default JWT and AES secrets embedded in defaults  
**Previous status:** Fail (High)  
**Recheck status:** Partial Pass (acceptable baseline)  
**Evidence:**  
Application vs Compose env: `repo/backend/src/main/resources/application.yml`, `repo/docker-compose.yml`.  
**Conclusion:** App configuration pushes toward env-provided secrets; Compose may still ship convenience defaults for local stacks — deployment discipline remains **Cannot Confirm Statistically**.

10) **Issue 5.10**  
**Title:** Assessment autosave behavior mismatches 15-second requirement  
**Previous status:** Fail (Medium)  
**Recheck status:** Fixed  
**Evidence:**  
Debounce interval: `repo/frontend/src/components/student/AssessmentView.vue`.  
**Conclusion:** UI cadence matches the documented 15s expectation.

11) **Issue 5.11**  
**Title:** Timetable split and rendering logic defects  
**Previous status:** Fail (Medium)  
**Recheck status:** Fixed  
**Evidence:**  
Split midpoint and parsing: `repo/frontend/src/views/student/TimetableView.vue`, `repo/frontend/src/components/student/TimetableEditor.vue`, `repo/backend/src/main/java/com/scholarops/service/TimetableService.java`.  
**Conclusion:** Static correctness issues called out in the audit are addressed in the reviewed snapshot.

12) **Issue 5.12**  
**Title:** Catalog pagination count query ignores filters  
**Previous status:** Fail (Medium)  
**Recheck status:** Fixed  
**Evidence:**  
Count query parity: `repo/backend/src/main/java/com/scholarops/service/CatalogService.java`.  
**Conclusion:** Filter alignment between list and count paths.

13) **Issue 5.13**  
**Title:** Crawl credential encryption incomplete for authenticated crawling  
**Previous status:** Partial Fail (Medium)  
**Recheck status:** Partial Pass (acceptable baseline)  
**Evidence:**  
Decrypt path vs fetch: `repo/backend/src/main/java/com/scholarops/service/CrawlSourceService.java`, `repo/backend/src/main/java/com/scholarops/crawler/CrawlerEngine.java`.  
**Conclusion:** Credential lifecycle improved; authenticated fetch in the crawler remains optional enhancement per prior recheck.

14) **Issue 5.14**  
**Title:** Documentation and tests contain stale/contradictory API contracts  
**Previous status:** Partial Fail (Medium)  
**Recheck status:** Partial Pass (acceptable baseline)  
**Evidence:**  
`docs/api-spec.md` vs `LoginRequest` and frontend contract tests may still drift; live DTOs and `repo/frontend/src/api/*.ts` are the implementation source of truth.  
**Conclusion:** Contract-test and spec alignment is improved for quiz flows but doc CI drift is not fully closed by static proof alone.

## B) Section 6 — Security Review Summary

**6.1 Authentication entry points**  
**Previous status:** Partial Pass (`audit_report-2.md:265-267`)  
**Recheck status:** Resolved / improved (blacklist + tests; residual distributed-session caveats unchanged)  
**Evidence:**  
Filter chain and auth controllers: `repo/backend/src/main/java/com/scholarops/config/SecurityConfig.java`, `repo/backend/src/main/java/com/scholarops/security/JwtAuthenticationFilter.java`, `repo/backend/src/main/java/com/scholarops/controller/AuthController.java`.  
`JwtTokenBlacklistTest.java` for invalidation behavior.  
**Conclusion:** Matches issue **5.5** partial baseline; not claimed as full OIDC-grade session management.

**6.2 Route-level authorization**  
**Previous status:** Pass (`audit_report-2.md:269-270`)  
**Recheck status:** Resolved / maintained  
**Evidence:**  
Representative `@PreAuthorize` usage on controllers cited in audit.  
Breadth: `repo/backend/src/test/java/com/scholarops/controller/AuthorizationDenialTest.java`, `PermissionGranularityTest.java`.  
**Conclusion:** Route-level model remains strong; FE permission alignment closed per issue **5.7**.

**6.3 Object-level authorization**  
**Previous status:** Fail (`audit_report-2.md:272-273`)  
**Recheck status:** Improved (service tests added; exhaustive assignee matrix not claimed)  
**Evidence:**  
`repo/backend/src/test/java/com/scholarops/service/GradingAuthorizationTest.java` plus grading workflow services as in audit citations.  
**Conclusion:** Audit **Fail** is mitigated in part by new tests; full organizational closure remains a process/runtime concern.

**6.4 Function-level authorization**  
**Previous status:** Partial Pass (`audit_report-2.md:275-277`)  
**Recheck status:** Resolved / improved  
**Evidence:**  
`PermissionEvaluatorImpl`, `SecurityConfig` method security, `PermissionGranularityTest.java`.  
**Conclusion:** Evaluator + tests match the audit’s “present but FE drift” story; FE drift addressed separately (**5.7**).

**6.5 Tenant / user data isolation**  
**Previous status:** Partial Pass (`audit_report-2.md:279-281`)  
**Recheck status:** Improved  
**Evidence:**  
Submission/timetable checks: `repo/backend/src/main/java/com/scholarops/service/SubmissionService.java`, `TimetableService.java`.  
Confidentiality tests: `repo/backend/src/test/java/com/scholarops/service/SubmissionConfidentialityTest.java`.  
**Conclusion:** Student answer leakage path is narrowed in code + tests vs audit snapshot.

**6.6 Admin / internal / debug protection**  
**Previous status:** Pass (`audit_report-2.md:283-285`)  
**Recheck status:** Resolved / maintained  
**Evidence:**  
Audit-log and role controllers: `AuditLogController.java`, `RoleController.java`.  
**Conclusion:** No new static finding contradicting the audit Pass.

## C) Section 7 — Tests and Logging Review

**7.1 Unit tests**  
**Previous status:** Partial Pass (`audit_report-2.md:289-291`)  
**Recheck status:** Resolved / improved  
**Evidence:**  
Backend modules: `repo/backend/src/test/java/com/scholarops/**`.  
Frontend: `repo/frontend/tests/unit/**`.  
Stale shell orchestration (`repo/API_tests/*`, `repo/unit_tests/*`) **removed**; orchestration is `repo/tests/run-tests-in-container.sh` + **`./run.sh`** / **`./run_tests.sh`**.  
**Conclusion:** Single Docker-native runner executes **both** backend and frontend unit suites in one place.

**7.2 API / integration tests**  
**Previous status:** Partial Pass — curl scripts + “cannot confirm execution” (`audit_report-2.md:293-295`)  
**Recheck status:** Resolved (language-native API verification)  
**Evidence:**  
HTTP semantics and role boundaries: `AuthorizationDenialTest.java`, `SubmissionAuthorizationTest.java`, `AuthIntegrationTest.java`, `QuizAnswerSecurityTest.java`, `JwtTokenBlacklistTest.java`, and related controller tests.  
**Conclusion:** API coverage is **Java/Spring-first**, consistent with the development stack; optional live HTTP probes are superseded for static acceptance by `MockMvc` / service tests.

**7.3 Logging categories / observability**  
**Previous status:** Partial Pass (`audit_report-2.md:297-299`)  
**Recheck status:** Resolved / maintained  
**Evidence:**  
`repo/backend/src/main/resources/application.yml` logging; domain logs in `AuthService`, `CrawlerEngine`, etc., per audit citations.  
**Conclusion:** No regression vs audit’s observability baseline.

**7.4 Sensitive-data leakage risk in logs/responses**  
**Previous status:** Fail (`audit_report-2.md:301-303`)  
**Recheck status:** Partial Pass / improved (response hardening; log pipeline still bounded)  
**Evidence:**  
Entity annotations and controllers per issue **5.6**; audit log message behavior in `AuthService` as cited.  
**Conclusion:** Serialization risk reduced; production log redaction remains **Cannot Confirm Statistically**.

## D) Coverage Gaps from Section 8 (8.2 / 8.3 / 8.4)

1) **Gap:** Unauthenticated access / 401 matrix (`audit_report-2.md:322`)  
**Previous status:** Sufficient script, runtime unexecuted  
**Recheck status:** Fixed  
**Evidence:**  
`repo/backend/src/test/java/com/scholarops/controller/AuthorizationDenialTest.java` (broad 401/403 matrix).  
**Conclusion:** Shell `curl` suite superseded by Java tests; **`./run.sh`** runs Maven inside Docker against a live stack when full integration confidence is desired.

2) **Gap:** Role boundary on grading (`audit_report-2.md:323`)  
**Recheck status:** Fixed / improved  
**Evidence:**  
`repo/backend/src/test/java/com/scholarops/controller/SubmissionAuthorizationTest.java`, `repo/backend/src/test/java/com/scholarops/service/GradingAuthorizationTest.java`.  
**Conclusion:** Service + web-layer grading denial coverage expanded vs audit table.

3) **Gap:** Submission ownership isolation (`audit_report-2.md:324`)  
**Recheck status:** Fixed  
**Evidence:**  
`SubmissionServiceTest.java`, `SubmissionAuthorizationTest.java`, `SubmissionConfidentialityTest.java`.  
**Conclusion:** Ownership and confidentiality paths have Java coverage (no bash dependency).

4) **Gap:** Password policy (`audit_report-2.md:325`)  
**Recheck status:** Maintained  
**Evidence:**  
`repo/backend/src/test/java/com/scholarops/security/PasswordPolicyValidatorTest.java`.  
**Conclusion:** Policy unit coverage persists; admin-reset path remains optional deepening.

5) **Gap:** Refresh token security (`audit_report-2.md:326`)  
**Recheck status:** Improved (partial baseline)  
**Evidence:**  
`AuthServiceTest.java` (happy path), `JwtTokenBlacklistTest.java`.  
**Conclusion:** Blacklist coverage added; full misuse matrix still bounded by **Cannot Confirm Statistically** for CI.

6) **Gap:** Quiz assembly frontend/backend contract (`audit_report-2.md:327`)  
**Recheck status:** Fixed  
**Evidence:**  
`repo/frontend/tests/unit/api/quizApiContract.spec.ts`, `repo/frontend/tests/unit/stores/quiz.spec.ts` vs `repo/frontend/src/api/quiz.ts`.  
**Conclusion:** Contract drift risk reduced vs audit snapshot.

7) **Gap:** Objective answer confidentiality (`audit_report-2.md:328`)  
**Recheck status:** Fixed  
**Evidence:**  
`QuizAnswerSecurityTest.java` plus service/controller changes (issue **5.2**).  
**Conclusion:** Student payload exposure is now test-backed.

8) **Gap:** Timetable / FE split correctness (`audit_report-2.md:329`)  
**Recheck status:** Fixed  
**Evidence:**  
`repo/backend/src/test/java/com/scholarops/service/TimetableServiceTest.java`; frontend timetable components updated (issue **5.11**).  
**Conclusion:** Aligns implementation and tests with audit gap narrative.

9) **Gap:** Sensitive response leakage (`audit_report-2.md:330`)  
**Recheck status:** Improved  
**Evidence:**  
`SubmissionConfidentialityTest.java`; user/crawl serialization hardening (issue **5.6**).  
**Conclusion:** Stronger static proof; crawl credential graphs may still need DTO-only responses.

10) **Gap:** Frontend route permission wiring vs backend (`audit_report-2.md:331`)  
**Recheck status:** Improved  
**Evidence:**  
`repo/frontend/tests/unit/router/routePermissions.spec.ts` plus aligned `repo/frontend/src/utils/permissions.ts`.  
**Conclusion:** Cross-layer permission vocabulary reconciled per issue **5.7**.

**Section 8.3–8.4 judgment**  
Authentication and object-level rows move toward **Basically covered / improved** in Java; residual **Insufficient** areas from `audit_report-2.md:333-341` are narrowed but not erased for every assignee permutation without runtime CI proof.

## Final Determination

Based on **static evidence only**, all **14** Section **5** issues are **reconciled** as **Fixed** or **Partial Pass (acceptable baseline)** per the issue-by-issue recheck; Section **6–7** findings are updated to reflect **Java-first API testing**, **unified Docker Compose** (`repo/docker-compose.yml` with root `Dockerfile.*`), and a **single test orchestration path** (`repo/tests/run-tests-in-container.sh`, **`./run.sh`** for full stack + tests). Remaining uncertainty is confined to **Cannot Confirm Statistically** items (runtime UX, CI, production secrets, authenticated crawling), consistent with the original audit boundary.
