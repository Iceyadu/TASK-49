# ScholarOps Ambiguity Resolutions

## 1. Price Range Filtering

Question:
How does "price range" filtering work for an offline system with no payment integration?

My Understanding:
The system has no payment gateway, checkout flow, or real currency transactions. However, the catalog filter includes a "priceRange" parameter with values like FREE, LOW, MEDIUM, and HIGH. This seems contradictory for an offline educational tool.

Solution:
"Price range" is repurposed as a **difficulty/effort tier** proxy. Each catalog item is assigned a difficulty tier (FREE = introductory/no prerequisites, LOW = beginner, MEDIUM = intermediate, HIGH = advanced) based on curator-assigned metadata. The filter parameter is named `priceRange` for UI label flexibility but maps internally to `difficultyTier`. No monetary value is stored or processed. The catalog endpoint accepts `priceRange` as a query parameter and translates it to a `difficultyTier` filter before querying. This keeps the filtering vocabulary generic so institutions can relabel it in the frontend without backend changes.

---

## 2. Drag-and-Drop Timetable Editing

Question:
How is drag-and-drop timetable editing represented when there is no live interactive frontend, only a static review of the API?

My Understanding:
The requirements mention drag-and-drop as a timetable editing paradigm, but the backend is a REST API that only deals with request/response cycles. There is no WebSocket or real-time state synchronization described.

Solution:
Drag-and-drop is a frontend-only interaction pattern. The API represents its result as a **PUT to `/api/timetable/slots/{slotId}`** with the new `dayOfWeek`, `startTime`, and `endTime`. When a user drags a slot from Monday 09:00-10:30 to Tuesday 10:00-11:30, the frontend issues a single PUT with the new coordinates. The backend validates overlap constraints and locked-period restrictions, then returns the updated slot. The timetable grid positions (`row`, `col`, `rowSpan`) are computed server-side for consistent rendering. No intermediate drag state is persisted. If the move is invalid, the backend returns a 400 or 409 error code and the frontend reverts the visual position.

---

## 3. Popularity Sort Definition

Question:
How is "popularity" sort defined for catalog items when there are no purchases, page views, or analytics integrations described?

My Understanding:
The catalog endpoint supports sorting by `popularity`, but the system has no analytics service, no view-count tracking middleware, and no purchase history.

Solution:
Popularity is a **composite score** (`popularityScore`, integer 0-100) computed from three signals that are already captured within the system: (1) the number of times the catalog item's ID appears as a `catalogItemId` in schedule entries across all students, (2) the number of quiz questions that reference content tagged with the same knowledge tags, and (3) curator-assigned boost weight (optional, via content metadata). The score is recalculated by a scheduled batch job (or on-demand when content is approved) and stored as a denormalized field on the catalog view. Sorting by `popularity` orders by this precomputed integer descending. This avoids any external analytics dependency while still reflecting genuine usage patterns within the platform.

---

## 4. Crawl Credential Encryption

Question:
How are crawl credentials encrypted at rest versus runtime key management?

My Understanding:
Crawl sources can store a `credentialRef` field (e.g., `vault:mit-ocw-key`). The system needs to protect these secrets both when stored in the database and when used during an active crawl run, but there is no external vault service explicitly specified in the architecture.

Solution:
Credentials use a **two-layer scheme**. At rest, the `credentialRef` is a reference pointer (not the secret itself). The actual secret is stored in an encrypted column in a dedicated `crawl_credentials` table using AES-256-GCM with a database encryption key (DEK). The DEK is itself encrypted by a key-encryption key (KEK) loaded from an environment variable at application startup and held only in memory. At runtime, when a crawl run starts, the service decrypts the credential into memory for the duration of the run and discards it when the run completes or is cancelled. The `credentialRef` string in the API response is always the vault-style reference, never the plaintext secret. Credential values are never included in audit logs, error logs, or API responses. If the KEK environment variable is absent at startup, the application refuses to start and logs a fatal configuration error.

---

## 5. Workstation ID for Admin Override

Question:
What constitutes a "workstation ID" for admin override logging?

My Understanding:
The API spec defines an `X-Workstation-Id` header for admin actions that gets recorded in audit logs. It is unclear what format this ID takes, whether it is validated, and what happens if it is missing.

Solution:
A workstation ID is a **free-form string identifier** (2-100 characters, alphanumeric plus hyphens and dots) that represents the physical or logical terminal from which an admin is operating (e.g., `WS-LAB-042`, `admin-office.north-3`, `kiosk-library-2F`). The value is not validated against any registry of known workstations; it is a self-reported label intended for audit traceability. The header is **required** for all endpoints that require the ADMIN role. If the header is missing or fails format validation on an admin request, the server returns 400 VALIDATION_ERROR with a message indicating the missing header. The workstation ID is stored verbatim in the audit log entry's `workstationId` field. Non-admin endpoints ignore the header if present.

---

## 6. Offline Plagiarism Checks

Question:
How do plagiarism checks work entirely offline without any external plagiarism detection service?

My Understanding:
The plagiarism endpoints allow instructors to check submissions for similarity, but the system is described as offline with no external API integrations to services like Turnitin or Moss.

Solution:
Plagiarism detection runs **locally using text-similarity algorithms** implemented within the application. The system supports four algorithms: COSINE_SIMILARITY (TF-IDF vector cosine distance), JACCARD_INDEX (set intersection over union on word tokens), LEVENSHTEIN_RATIO (normalized edit distance), and N_GRAM_OVERLAP (configurable n-gram shingling with overlap coefficient). When a check is initiated, the server extracts text answers from the specified submissions, preprocesses them (lowercasing, stopword removal, tokenization), and runs the selected algorithm pairwise across all submissions in the set. Pairs that exceed the configured `threshold` are flagged. All computation is CPU-bound on the application server. For large submission sets the check runs asynchronously (status transitions: PENDING -> RUNNING -> COMPLETED/FAILED). No text is sent to any external service. The matched fragments in the results are extracted by finding the longest common subsequences between flagged pairs.

---

## 7. Timezone Selection for Timestamp Normalization

Question:
How does timezone selection work for timestamp normalization across schedules and timetables?

My Understanding:
Schedules have a `timezone` field (IANA identifier), but the system also stores timestamps in ISO 8601 with Z (UTC) suffix. It is unclear how local times in schedule entries relate to UTC-stored audit/system timestamps.

Solution:
The system uses a **two-tier timestamp model**. All system-level timestamps (audit logs, createdAt, updatedAt, submission times, grading times) are stored and returned in **UTC (Z suffix)**. Schedule entries and timetable slots use **wall-clock local times** (`HH:mm` format without timezone offset) that are interpreted in the context of the schedule's `timezone` field. The schedule's `timezone` field is a required IANA identifier (e.g., `America/New_York`, `Europe/London`) set at schedule creation time. When the system needs to convert between the two (e.g., to check if a quiz submission time falls within a student's scheduled study block), it uses the schedule's timezone to convert the wall-clock time to UTC for comparison. The `weekOf` parameter on the timetable endpoint accepts an ISO date and is interpreted in the schedule's timezone. Daylight saving transitions are handled by the standard IANA timezone database; if a wall-clock time is ambiguous or skipped due to DST, the system uses the earlier offset for ambiguous times and shifts to the next valid minute for skipped times.

---

## 8. Hot-Update for Crawl Rules

Question:
What does "hot-update" mean for crawl rules versus normal version creation?

My Understanding:
Crawl rules have a `hotUpdate` boolean field and a `version` integer. The distinction between a hot-update and a normal update is not explicitly defined in terms of system behavior.

Solution:
A **normal update** (hotUpdate = false) increments the rule's version number and takes effect only on the **next** crawl run. Any currently running crawl continues using the rule version that was snapshotted when the run started. A **hot-update** (hotUpdate = true) also increments the version number but additionally signals the crawl engine to **reload the rule immediately** in any active crawl run for that source. The crawl engine maintains an in-memory rule set per active run; on hot-update, it receives a notification (via an internal event/message) to swap in the new rule version without restarting the run. This is useful for emergency corrections, such as adding an EXCLUDE pattern for a URL path that is causing errors during a long-running crawl. The `hotUpdate` flag is a request-time parameter, not a persistent property of the rule; it controls the behavior of that specific PUT operation. In the rule's stored record, the `hotUpdate` field reflects whether the most recent edit was a hot-update, for audit purposes.

---

## 9. Autosave and Submission Attempt Limits

Question:
How does autosave interact with submission attempt limits?

My Understanding:
Students can save answers in progress via PUT to the answers endpoint (autosave), and quizzes have a `maxAttempts` limit. It is ambiguous whether autosaves count as attempts or whether an expired-but-not-submitted attempt counts against the limit.

Solution:
Autosaves **never** count as attempts. The attempt count increments only when a student calls **POST to start a new submission** (creating an IN_PROGRESS submission). The autosave endpoint (PUT answers) simply persists the current answer state to the existing IN_PROGRESS submission without changing its status or attempt number. A submission that is started but never explicitly submitted (via POST submit) is **auto-submitted by the server** when the time limit expires (expiresAt is reached). This auto-submission transitions the status from IN_PROGRESS to SUBMITTED and counts as a used attempt. Therefore: (1) creating a submission increments the attempt counter, (2) autosaving does not, (3) explicitly submitting finalizes the attempt, (4) expiration also finalizes the attempt, and (5) there is no way to "cancel" a started attempt to reclaim it. If a student has used all attempts, POST to create a new submission returns 409 CONFLICT.

---

## 10. Merge/Split Sessions in Timetable

Question:
How do merge and split sessions work in the timetable?

My Understanding:
The timetable has merge and split endpoints, but the exact semantics around IDs, metadata inheritance, and constraints are not fully specified.

Solution:
**Split** takes a single slot and a `splitAt` time, producing two new slots. The original slot is deleted and two new slots are created with new UUIDs. The first slot inherits the original's startTime through splitAt; the second slot runs from splitAt through the original's endTime. Both inherit the title (with "(1/2)" and "(2/2)" suffixes), category, catalogItemId, and notes from the original. The split point must be strictly between startTime and endTime (not equal to either boundary). Minimum resulting slot duration is 15 minutes; the server rejects splits that would produce a shorter slot.

**Merge** takes exactly two slot IDs. The slots must be on the same day, must be temporally adjacent (one's endTime equals the other's startTime, with zero gap), and must share the same category. The two original slots are deleted and a single new slot is created spanning from the earlier startTime to the later endTime. The merged slot's title is taken from the first (earlier) slot; notes are concatenated with a newline separator. The catalogItemId is taken from the first slot (if they differ, the second is discarded with a warning in the response).

Both operations are blocked if any involved slot falls within a locked period (returns 409 LOCKED_PERIOD_CONFLICT).

---

## 11. Locked Period Overlap with Existing Entries

Question:
What happens when a locked period overlaps an existing schedule entry?

My Understanding:
Students can create locked periods that may cover time ranges where schedule entries already exist. The behavior for these pre-existing entries is not explicitly defined.

Solution:
When a locked period is created that overlaps existing schedule entries, those entries are **preserved in place but become immovable**. Specifically: (1) existing entries that fall fully or partially within the new locked period are not deleted, modified, or split; (2) any subsequent attempt to PUT (move/resize) those entries via the timetable slot endpoint returns 409 LOCKED_PERIOD_CONFLICT; (3) any attempt to DELETE those entries also returns 409 LOCKED_PERIOD_CONFLICT; (4) new entries cannot be created within the locked period (returns 409 LOCKED_PERIOD_CONFLICT); (5) the entries remain visible in the timetable with a `locked: true` flag for frontend rendering. When the locked period is deleted, all previously locked entries revert to normal editable status (`locked: false`). A single entry that partially overlaps a locked period (e.g., entry is 09:00-10:30, locked period is 10:00-12:00) is treated as fully locked -- the entire entry becomes immovable, not just the overlapping portion. This prevents partial-move scenarios that would be confusing.

---

## 12. Mandatory Rubric-Based Scoring

Question:
How is rubric-based scoring enforced as "mandatory"?

My Understanding:
The grading endpoint requires rubric selections for SHORT_ANSWER and ESSAY questions, but the exact enforcement rules and the relationship between rubric item points and the overall points awarded are ambiguous.

Solution:
Rubric-based scoring is mandatory for SHORT_ANSWER and ESSAY question types through **strict server-side validation** on the grading endpoint. The rules are: (1) the `rubricSelections` array must include an entry for **every** rubric item defined on the question -- omitting any item returns 400 VALIDATION_ERROR; (2) each rubric selection sets `satisfied` to true or false; (3) the `pointsAwarded` value must **exactly equal** the sum of points from all rubric items where `satisfied` is true -- any mismatch returns 400 VALIDATION_ERROR with a message showing the expected vs. provided value; (4) rubric items are defined at question creation time and their point values must sum to the question's total `points` value; (5) for MULTIPLE_CHOICE, MULTIPLE_SELECT, and TRUE_FALSE questions, rubric selections are ignored (auto-graded); (6) the `feedback` field is optional but recommended; (7) a grader cannot finalize a submission (POST finalize) until all SHORT_ANSWER and ESSAY questions have been graded with valid rubric selections. This design ensures that grading is consistent, auditable, and not arbitrarily assigned -- every point awarded traces back to a specific rubric criterion.
