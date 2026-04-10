# ScholarOps API Specification

## Auth Model Summary

- **Authentication method:** JWT Bearer token
- **Access token expiry:** 1 hour
- **Refresh token expiry:** 24 hours
- **Header format:** `Authorization: Bearer <token>`
- **Admin override header:** `X-Workstation-Id: <id>` (logged in audit trail for all admin actions)
- All endpoints except login and refresh require a valid access token.
- Role-based access control (RBAC) is enforced per endpoint.

---

## Response Shapes

### Success Response

```json
{
  "success": true,
  "data": { ... },
  "meta": { ... }
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Human-readable description of the problem",
    "details": [
      {
        "field": "email",
        "reason": "must be a valid email address"
      }
    ]
  }
}
```

### Paged Response

```json
{
  "success": true,
  "data": [ ... ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

Pagination query parameters accepted by all paged endpoints:

| Param  | Type    | Default | Description              |
|--------|---------|---------|--------------------------|
| page   | integer | 0       | Zero-based page index    |
| size   | integer | 20      | Items per page (max 100) |
| sort   | string  | varies  | Field and direction, e.g. `name,asc` |

---

## Error Codes

| HTTP Status | Code                      | Description                                      |
|-------------|---------------------------|--------------------------------------------------|
| 400         | VALIDATION_ERROR          | Request body or query param fails validation      |
| 400         | PASSWORD_POLICY_VIOLATION | Password does not meet strength requirements      |
| 401         | UNAUTHORIZED              | Missing, expired, or invalid token                |
| 403         | FORBIDDEN                 | Authenticated user lacks the required role         |
| 404         | RESOURCE_NOT_FOUND        | Entity with the given ID does not exist           |
| 409         | CONFLICT                  | Unique constraint violation (e.g. duplicate email) |
| 409         | LOCKED_PERIOD_CONFLICT    | Action conflicts with a locked time period         |
| 429         | RATE_LIMIT_EXCEEDED       | Too many requests from this client                 |
| 500         | INTERNAL_ERROR            | Unexpected server-side failure                     |

---

## Endpoints

---

### Auth (3 endpoints)

#### POST /api/auth/login (Public)

Authenticate a user and receive token pair.

**Request:**

```json
{
  "email": "user@example.com",
  "password": "s3cureP@ss!"
}
```

**Validation:**

- `email` — required, valid email format
- `password` — required, non-blank

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "dGhpcyBpcyBh...",
    "expiresIn": 3600,
    "tokenType": "Bearer",
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "displayName": "Jane Doe",
      "roles": ["STUDENT"]
    }
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED

---

#### POST /api/auth/refresh (Public)

Exchange a valid refresh token for a new token pair.

**Request:**

```json
{
  "refreshToken": "dGhpcyBpcyBh..."
}
```

**Validation:**

- `refreshToken` — required, non-blank

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "bmV3IHJlZnJl...",
    "expiresIn": 3600,
    "tokenType": "Bearer"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED

---

#### POST /api/auth/logout (Authenticated)

Invalidate the current refresh token.

**Request:**

```json
{
  "refreshToken": "dGhpcyBpcyBh..."
}
```

**Validation:**

- `refreshToken` — required

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "message": "Logged out successfully"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED

---

### Users (7 endpoints, ADMIN only)

All user endpoints require the `ADMIN` role. The `X-Workstation-Id` header is logged for audit.

#### GET /api/users (ADMIN)

List all users with pagination.

**Query Parameters:** `page`, `size`, `sort`, `search` (optional, filters by name or email)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "email": "user@example.com",
      "displayName": "Jane Doe",
      "roles": ["STUDENT"],
      "enabled": true,
      "createdAt": "2026-01-15T10:30:00Z",
      "updatedAt": "2026-03-01T14:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 52, "totalPages": 3 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/users/{id} (ADMIN)

Retrieve a single user by ID.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "displayName": "Jane Doe",
    "roles": ["STUDENT"],
    "enabled": true,
    "createdAt": "2026-01-15T10:30:00Z",
    "updatedAt": "2026-03-01T14:00:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/users (ADMIN)

Create a new user account.

**Request:**

```json
{
  "email": "newuser@example.com",
  "displayName": "John Smith",
  "password": "Str0ng!Pass",
  "roleIds": ["uuid-role-1"]
}
```

**Validation:**

- `email` — required, valid email, unique across system
- `displayName` — required, 2-100 characters
- `password` — required, minimum 8 characters, must contain uppercase, lowercase, digit, and special character
- `roleIds` — required, non-empty array of valid role UUIDs

**Success Response (201):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "newuser@example.com",
    "displayName": "John Smith",
    "roles": ["STUDENT"],
    "enabled": true,
    "createdAt": "2026-04-09T08:00:00Z",
    "updatedAt": "2026-04-09T08:00:00Z"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 400 PASSWORD_POLICY_VIOLATION, 401 UNAUTHORIZED, 403 FORBIDDEN, 409 CONFLICT

---

#### PUT /api/users/{id} (ADMIN)

Update an existing user (partial updates allowed).

**Request:**

```json
{
  "displayName": "John Q. Smith",
  "email": "jsmith@example.com",
  "roleIds": ["uuid-role-1", "uuid-role-2"],
  "enabled": true
}
```

**Validation:**

- `email` — if provided, valid email, unique
- `displayName` — if provided, 2-100 characters
- `roleIds` — if provided, non-empty array of valid role UUIDs

**Success Response (200):** Same shape as GET /api/users/{id}

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### PUT /api/users/{id}/password (ADMIN)

Reset a user's password.

**Request:**

```json
{
  "newPassword": "N3w!Passw0rd"
}
```

**Validation:**

- `newPassword` — required, minimum 8 characters, must contain uppercase, lowercase, digit, and special character

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "message": "Password updated successfully"
  }
}
```

**Error Codes:** 400 PASSWORD_POLICY_VIOLATION, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### PUT /api/users/{id}/enable (ADMIN)

Enable a user account.

**Request:** None (empty body)

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "enabled": true
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### PUT /api/users/{id}/disable (ADMIN)

Disable a user account. Disabled users cannot authenticate.

**Request:** None (empty body)

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "enabled": false
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Roles (3 endpoints, ADMIN only)

#### GET /api/roles (ADMIN)

List all roles.

**Success Response (200):**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "ADMIN",
      "description": "Full system administrator",
      "permissions": ["USER_MANAGE", "ROLE_MANAGE", "AUDIT_READ"],
      "createdAt": "2026-01-01T00:00:00Z"
    }
  ]
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/roles/{id} (ADMIN)

Retrieve a single role by ID.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "INSTRUCTOR",
    "description": "Course instructor with quiz management",
    "permissions": ["QUIZ_MANAGE", "CONTENT_READ", "GRADE_MANAGE"],
    "createdAt": "2026-01-01T00:00:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/roles (ADMIN)

Create a new role.

**Request:**

```json
{
  "name": "CURATOR",
  "description": "Content curator who manages crawled material",
  "permissions": ["CRAWL_MANAGE", "CONTENT_MANAGE"]
}
```

**Validation:**

- `name` — required, 2-50 characters, uppercase alphanumeric and underscores, unique
- `description` — optional, max 255 characters
- `permissions` — required, non-empty array of valid permission strings

**Success Response (201):** Same shape as GET /api/roles/{id}

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 409 CONFLICT

---

### Audit (2 endpoints, ADMIN only)

#### GET /api/audit/logs (ADMIN)

Retrieve paginated audit log entries.

**Query Parameters:** `page`, `size`, `sort`, `userId` (optional), `action` (optional), `from` (ISO datetime), `to` (ISO datetime)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "userId": "uuid",
      "userEmail": "admin@example.com",
      "action": "USER_CREATED",
      "resourceType": "USER",
      "resourceId": "uuid",
      "workstationId": "WS-LAB-042",
      "details": { "email": "newuser@example.com" },
      "timestamp": "2026-04-09T08:15:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 340, "totalPages": 17 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/audit/logs/{id} (ADMIN)

Retrieve a single audit log entry by ID.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "userEmail": "admin@example.com",
    "action": "USER_CREATED",
    "resourceType": "USER",
    "resourceId": "uuid",
    "workstationId": "WS-LAB-042",
    "details": { "email": "newuser@example.com" },
    "timestamp": "2026-04-09T08:15:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Crawl Sources (5 endpoints, CURATOR)

#### GET /api/crawl-sources (CURATOR)

List all crawl sources with pagination.

**Query Parameters:** `page`, `size`, `sort`, `type` (optional: WEB, FTP, API), `enabled` (optional: true/false)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "MIT OCW Computer Science",
      "type": "WEB",
      "baseUrl": "https://ocw.mit.edu/courses/cs/",
      "enabled": true,
      "credentialRef": "vault:mit-ocw-key",
      "lastCrawledAt": "2026-04-08T02:00:00Z",
      "createdAt": "2026-02-10T09:00:00Z",
      "updatedAt": "2026-04-08T02:30:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 8, "totalPages": 1 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/crawl-sources/{id} (CURATOR)

Retrieve a single crawl source.

**Success Response (200):** Single object with same shape as list item.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/crawl-sources (CURATOR)

Create a new crawl source.

**Request:**

```json
{
  "name": "Stanford NLP Resources",
  "type": "WEB",
  "baseUrl": "https://nlp.stanford.edu/pubs/",
  "enabled": true,
  "credentialRef": "vault:stanford-api-key",
  "config": {
    "maxDepth": 3,
    "respectRobotsTxt": true,
    "rateLimitMs": 1000
  }
}
```

**Validation:**

- `name` — required, 2-200 characters, unique
- `type` — required, one of: WEB, FTP, API
- `baseUrl` — required, valid URL format
- `enabled` — optional, defaults to true
- `credentialRef` — optional, vault reference string
- `config.maxDepth` — optional, integer 1-10, default 3
- `config.rateLimitMs` — optional, integer >= 100

**Success Response (201):** Same shape as GET /api/crawl-sources/{id}

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 409 CONFLICT

---

#### PUT /api/crawl-sources/{id} (CURATOR)

Update an existing crawl source.

**Request:** Same shape as POST, all fields optional.

**Validation:** Same rules as POST for any provided fields.

**Success Response (200):** Same shape as GET /api/crawl-sources/{id}

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### DELETE /api/crawl-sources/{id} (CURATOR)

Delete a crawl source. Fails if active crawl runs reference this source.

**Success Response (204):** No body.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

### Crawl Rules (5 endpoints, CURATOR)

#### GET /api/crawl-sources/{sourceId}/rules (CURATOR)

List all rules for a crawl source.

**Query Parameters:** `page`, `size`, `sort`

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "sourceId": "uuid",
      "pattern": ".*\\.pdf$",
      "action": "INCLUDE",
      "priority": 10,
      "version": 3,
      "hotUpdate": false,
      "createdAt": "2026-03-01T10:00:00Z",
      "updatedAt": "2026-04-01T12:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 5, "totalPages": 1 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### GET /api/crawl-sources/{sourceId}/rules/{ruleId} (CURATOR)

Retrieve a single crawl rule.

**Success Response (200):** Single rule object.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/crawl-sources/{sourceId}/rules (CURATOR)

Create a new crawl rule.

**Request:**

```json
{
  "pattern": ".*\\.(jpg|png|gif)$",
  "action": "EXCLUDE",
  "priority": 20,
  "hotUpdate": false
}
```

**Validation:**

- `pattern` — required, valid regex, max 500 characters
- `action` — required, one of: INCLUDE, EXCLUDE, TRANSFORM
- `priority` — required, integer 1-100, unique within the source
- `hotUpdate` — optional, boolean, defaults to false

**Success Response (201):** Single rule object.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### PUT /api/crawl-sources/{sourceId}/rules/{ruleId} (CURATOR)

Update a crawl rule. Increments the version automatically. If `hotUpdate` is true, the rule takes effect immediately on any active crawl run without restarting.

**Request:** Same shape as POST, all fields optional.

**Success Response (200):** Single rule object with incremented version.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### DELETE /api/crawl-sources/{sourceId}/rules/{ruleId} (CURATOR)

Delete a crawl rule.

**Success Response (204):** No body.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Crawl Runs (4 endpoints, CURATOR)

#### GET /api/crawl-runs (CURATOR)

List crawl runs with pagination.

**Query Parameters:** `page`, `size`, `sort`, `sourceId` (optional), `status` (optional: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "sourceId": "uuid",
      "sourceName": "MIT OCW Computer Science",
      "status": "COMPLETED",
      "startedAt": "2026-04-08T02:00:00Z",
      "completedAt": "2026-04-08T02:28:00Z",
      "itemsDiscovered": 142,
      "itemsProcessed": 138,
      "itemsFailed": 4,
      "createdAt": "2026-04-08T01:59:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 25, "totalPages": 2 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/crawl-runs/{id} (CURATOR)

Retrieve a single crawl run with detailed statistics.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "sourceId": "uuid",
    "sourceName": "MIT OCW Computer Science",
    "status": "COMPLETED",
    "startedAt": "2026-04-08T02:00:00Z",
    "completedAt": "2026-04-08T02:28:00Z",
    "itemsDiscovered": 142,
    "itemsProcessed": 138,
    "itemsFailed": 4,
    "errorLog": [
      { "url": "https://example.com/broken", "error": "HTTP 404", "timestamp": "2026-04-08T02:15:00Z" }
    ],
    "createdAt": "2026-04-08T01:59:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/crawl-runs (CURATOR)

Trigger a new crawl run for a source.

**Request:**

```json
{
  "sourceId": "uuid",
  "mode": "FULL",
  "maxItems": 500
}
```

**Validation:**

- `sourceId` — required, must reference an enabled crawl source
- `mode` — required, one of: FULL, INCREMENTAL
- `maxItems` — optional, integer 1-10000

**Success Response (201):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "sourceId": "uuid",
    "status": "PENDING",
    "createdAt": "2026-04-09T08:00:00Z"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### POST /api/crawl-runs/{id}/cancel (CURATOR)

Cancel a running or pending crawl run.

**Request:** None (empty body).

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "CANCELLED"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR (if already completed), 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Content (5 endpoints, CURATOR)

#### GET /api/content (CURATOR)

List crawled content items with pagination.

**Query Parameters:** `page`, `size`, `sort`, `sourceId` (optional), `status` (optional: RAW, PROCESSED, APPROVED, REJECTED), `search` (optional, full-text on title)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "sourceId": "uuid",
      "title": "Introduction to Algorithms - Lecture 1",
      "contentType": "PDF",
      "status": "PROCESSED",
      "originUrl": "https://ocw.mit.edu/courses/cs/6-006/lecture1.pdf",
      "fileSizeBytes": 2048576,
      "tags": ["algorithms", "computer-science"],
      "createdAt": "2026-04-08T02:10:00Z",
      "updatedAt": "2026-04-08T03:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 138, "totalPages": 7 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/content/{id} (CURATOR)

Retrieve a single content item with full metadata.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "sourceId": "uuid",
    "title": "Introduction to Algorithms - Lecture 1",
    "contentType": "PDF",
    "status": "PROCESSED",
    "originUrl": "https://ocw.mit.edu/courses/cs/6-006/lecture1.pdf",
    "fileSizeBytes": 2048576,
    "textPreview": "First 500 characters of extracted text...",
    "tags": ["algorithms", "computer-science"],
    "metadata": {
      "author": "Erik Demaine",
      "pageCount": 24,
      "language": "en"
    },
    "crawlRunId": "uuid",
    "createdAt": "2026-04-08T02:10:00Z",
    "updatedAt": "2026-04-08T03:00:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### PUT /api/content/{id} (CURATOR)

Update content metadata (title, tags, status).

**Request:**

```json
{
  "title": "Intro to Algorithms - Lecture 1 (Revised)",
  "tags": ["algorithms", "computer-science", "mit"],
  "status": "APPROVED"
}
```

**Validation:**

- `title` — if provided, 2-500 characters
- `tags` — if provided, array of strings, each 1-50 characters, max 20 tags
- `status` — if provided, one of: RAW, PROCESSED, APPROVED, REJECTED

**Success Response (200):** Same shape as GET /api/content/{id}

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/content/{id}/approve (CURATOR)

Mark content as approved for catalog visibility.

**Request:** None (empty body).

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "APPROVED"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/content/{id}/reject (CURATOR)

Mark content as rejected.

**Request:**

```json
{
  "reason": "Duplicate of existing content item"
}
```

**Validation:**

- `reason` — optional, max 500 characters

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "REJECTED"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Catalog (2 endpoints, STUDENT / INSTRUCTOR)

#### GET /api/catalog (STUDENT, INSTRUCTOR)

Browse approved content in the public catalog.

**Query Parameters:** `page`, `size`, `sort` (name, popularity, date), `search` (full-text), `tags` (comma-separated), `contentType` (optional), `priceRange` (optional: FREE, LOW, MEDIUM, HIGH)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "title": "Introduction to Algorithms - Lecture 1",
      "contentType": "PDF",
      "tags": ["algorithms", "computer-science"],
      "popularityScore": 87,
      "difficultyTier": "MEDIUM",
      "estimatedMinutes": 45,
      "createdAt": "2026-04-08T02:10:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 95, "totalPages": 5 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/catalog/{id} (STUDENT, INSTRUCTOR)

Retrieve full catalog item details.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "title": "Introduction to Algorithms - Lecture 1",
    "contentType": "PDF",
    "tags": ["algorithms", "computer-science"],
    "popularityScore": 87,
    "difficultyTier": "MEDIUM",
    "estimatedMinutes": 45,
    "textPreview": "First 500 characters...",
    "metadata": {
      "author": "Erik Demaine",
      "pageCount": 24,
      "language": "en"
    },
    "relatedItems": ["uuid-1", "uuid-2"],
    "createdAt": "2026-04-08T02:10:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Question Banks (7 endpoints, INSTRUCTOR)

#### GET /api/question-banks (INSTRUCTOR)

List all question banks with pagination.

**Query Parameters:** `page`, `size`, `sort`, `search` (optional), `tagId` (optional)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Data Structures Midterm Pool",
      "description": "Questions covering chapters 1-5",
      "questionCount": 48,
      "tags": [{ "id": "uuid", "name": "data-structures" }],
      "createdBy": "uuid",
      "createdAt": "2026-03-15T09:00:00Z",
      "updatedAt": "2026-04-05T16:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 12, "totalPages": 1 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/question-banks/{id} (INSTRUCTOR)

Retrieve a question bank with its questions.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Data Structures Midterm Pool",
    "description": "Questions covering chapters 1-5",
    "questions": [
      {
        "id": "uuid",
        "text": "What is the time complexity of binary search?",
        "type": "MULTIPLE_CHOICE",
        "options": [
          { "id": "a", "text": "O(n)", "correct": false },
          { "id": "b", "text": "O(log n)", "correct": true },
          { "id": "c", "text": "O(n log n)", "correct": false },
          { "id": "d", "text": "O(1)", "correct": false }
        ],
        "points": 5,
        "difficulty": "EASY",
        "tags": [{ "id": "uuid", "name": "searching" }],
        "explanation": "Binary search halves the search space each step."
      }
    ],
    "questionCount": 48,
    "tags": [{ "id": "uuid", "name": "data-structures" }],
    "createdBy": "uuid",
    "createdAt": "2026-03-15T09:00:00Z",
    "updatedAt": "2026-04-05T16:00:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/question-banks (INSTRUCTOR)

Create a new question bank.

**Request:**

```json
{
  "name": "Algorithms Final Exam Pool",
  "description": "Comprehensive question set for final examination",
  "tagIds": ["uuid-tag-1"]
}
```

**Validation:**

- `name` — required, 2-200 characters, unique per instructor
- `description` — optional, max 1000 characters
- `tagIds` — optional, array of valid tag UUIDs

**Success Response (201):** Same shape as GET /api/question-banks/{id} (with empty questions array).

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 409 CONFLICT

---

#### PUT /api/question-banks/{id} (INSTRUCTOR)

Update question bank metadata.

**Request:** Same fields as POST, all optional.

**Success Response (200):** Same shape as GET /api/question-banks/{id}.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### DELETE /api/question-banks/{id} (INSTRUCTOR)

Delete a question bank. Fails if any quiz references questions from this bank.

**Success Response (204):** No body.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### POST /api/question-banks/{id}/questions (INSTRUCTOR)

Add a question to a bank.

**Request:**

```json
{
  "text": "Explain the difference between a stack and a queue.",
  "type": "SHORT_ANSWER",
  "points": 10,
  "difficulty": "MEDIUM",
  "tagIds": ["uuid-tag-1"],
  "explanation": "A stack is LIFO; a queue is FIFO.",
  "rubricItems": [
    { "criterion": "Mentions LIFO for stack", "points": 3 },
    { "criterion": "Mentions FIFO for queue", "points": 3 },
    { "criterion": "Provides a concrete example", "points": 4 }
  ]
}
```

**Validation:**

- `text` — required, 1-5000 characters
- `type` — required, one of: MULTIPLE_CHOICE, MULTIPLE_SELECT, TRUE_FALSE, SHORT_ANSWER, ESSAY
- `points` — required, integer 1-100
- `difficulty` — required, one of: EASY, MEDIUM, HARD
- `options` — required if type is MULTIPLE_CHOICE or MULTIPLE_SELECT (min 2, max 10); exactly one correct for MULTIPLE_CHOICE, at least one for MULTIPLE_SELECT
- `rubricItems` — required if type is SHORT_ANSWER or ESSAY; sum of rubric points must equal `points`
- `tagIds` — optional, array of valid tag UUIDs

**Success Response (201):** Single question object.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### DELETE /api/question-banks/{bankId}/questions/{questionId} (INSTRUCTOR)

Remove a question from a bank.

**Success Response (204):** No body.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

### Knowledge Tags (2 endpoints, INSTRUCTOR)

#### GET /api/knowledge-tags (INSTRUCTOR)

List all knowledge tags.

**Query Parameters:** `page`, `size`, `search` (optional, filters by name)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "binary-search",
      "description": "Binary search algorithm concepts",
      "usageCount": 14,
      "createdAt": "2026-02-20T11:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 35, "totalPages": 2 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### POST /api/knowledge-tags (INSTRUCTOR)

Create a new knowledge tag.

**Request:**

```json
{
  "name": "dynamic-programming",
  "description": "Dynamic programming techniques and patterns"
}
```

**Validation:**

- `name` — required, 2-100 characters, lowercase alphanumeric and hyphens, unique
- `description` — optional, max 500 characters

**Success Response (201):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "dynamic-programming",
    "description": "Dynamic programming techniques and patterns",
    "usageCount": 0,
    "createdAt": "2026-04-09T08:00:00Z"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 409 CONFLICT

---

### Quizzes (5 endpoints, INSTRUCTOR / STUDENT)

#### GET /api/quizzes (INSTRUCTOR, STUDENT)

List quizzes. Instructors see all their quizzes; students see only published quizzes assigned to them.

**Query Parameters:** `page`, `size`, `sort`, `status` (optional: DRAFT, PUBLISHED, CLOSED)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "title": "Data Structures Midterm",
      "description": "Covers chapters 1-5",
      "status": "PUBLISHED",
      "totalPoints": 100,
      "questionCount": 20,
      "timeLimitMinutes": 60,
      "maxAttempts": 2,
      "shuffleQuestions": true,
      "availableFrom": "2026-04-10T09:00:00Z",
      "availableUntil": "2026-04-10T10:00:00Z",
      "createdBy": "uuid",
      "createdAt": "2026-04-01T14:00:00Z",
      "updatedAt": "2026-04-08T10:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 6, "totalPages": 1 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/quizzes/{id} (INSTRUCTOR, STUDENT)

Retrieve quiz details. For students, correct answers and explanations are hidden until the quiz is closed or their submission is graded.

**Success Response (200):** Full quiz object including nested questions (answers redacted for students).

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/quizzes (INSTRUCTOR)

Create a new quiz.

**Request:**

```json
{
  "title": "Algorithms Quiz 3",
  "description": "Graph algorithms and shortest paths",
  "timeLimitMinutes": 45,
  "maxAttempts": 1,
  "shuffleQuestions": true,
  "questionIds": ["uuid-q1", "uuid-q2", "uuid-q3"],
  "availableFrom": "2026-04-15T09:00:00Z",
  "availableUntil": "2026-04-15T10:00:00Z"
}
```

**Validation:**

- `title` — required, 2-200 characters
- `timeLimitMinutes` — optional, integer 1-480
- `maxAttempts` — optional, integer 1-10, default 1
- `shuffleQuestions` — optional, boolean, default false
- `questionIds` — required, non-empty array of valid question UUIDs
- `availableFrom` — optional, ISO 8601 datetime, must be in the future
- `availableUntil` — optional, must be after `availableFrom`

**Success Response (201):** Full quiz object.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### PUT /api/quizzes/{id} (INSTRUCTOR)

Update a quiz. Only DRAFT quizzes can have their questions modified; PUBLISHED quizzes can only update availability window.

**Request:** Same fields as POST, all optional.

**Success Response (200):** Full quiz object.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### POST /api/quizzes/{id}/publish (INSTRUCTOR)

Publish a draft quiz, making it visible to students.

**Request:** None (empty body).

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "PUBLISHED"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR (if no questions), 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Submissions (5 endpoints, STUDENT)

#### GET /api/quizzes/{quizId}/submissions (STUDENT)

List the current student's submissions for a quiz.

**Success Response (200):**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "quizId": "uuid",
      "attemptNumber": 1,
      "status": "GRADED",
      "score": 85,
      "totalPoints": 100,
      "startedAt": "2026-04-10T09:01:00Z",
      "submittedAt": "2026-04-10T09:45:00Z",
      "gradedAt": "2026-04-11T14:00:00Z"
    }
  ]
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### GET /api/quizzes/{quizId}/submissions/{submissionId} (STUDENT)

Retrieve a single submission with answers. Correct answers visible only if graded.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "quizId": "uuid",
    "attemptNumber": 1,
    "status": "GRADED",
    "score": 85,
    "totalPoints": 100,
    "answers": [
      {
        "questionId": "uuid",
        "questionText": "What is the time complexity of binary search?",
        "selectedOptionId": "b",
        "textAnswer": null,
        "pointsAwarded": 5,
        "pointsPossible": 5,
        "feedback": "Correct!"
      }
    ],
    "startedAt": "2026-04-10T09:01:00Z",
    "submittedAt": "2026-04-10T09:45:00Z",
    "gradedAt": "2026-04-11T14:00:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/quizzes/{quizId}/submissions (STUDENT)

Start a new quiz attempt. Fails if max attempts exceeded or quiz not available.

**Request:** None (empty body).

**Validation:**

- Quiz must be PUBLISHED
- Current time must be within availability window
- Student must not have exceeded `maxAttempts`

**Success Response (201):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "quizId": "uuid",
    "attemptNumber": 2,
    "status": "IN_PROGRESS",
    "questions": [
      {
        "questionId": "uuid",
        "text": "What is the time complexity of binary search?",
        "type": "MULTIPLE_CHOICE",
        "options": [
          { "id": "a", "text": "O(n)" },
          { "id": "b", "text": "O(log n)" },
          { "id": "c", "text": "O(n log n)" },
          { "id": "d", "text": "O(1)" }
        ],
        "points": 5
      }
    ],
    "startedAt": "2026-04-10T09:01:00Z",
    "expiresAt": "2026-04-10T09:46:00Z"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### PUT /api/quizzes/{quizId}/submissions/{submissionId}/answers (STUDENT)

Save (autosave) answers for an in-progress submission. Can be called multiple times. Does not count as final submission.

**Request:**

```json
{
  "answers": [
    { "questionId": "uuid", "selectedOptionId": "b" },
    { "questionId": "uuid", "textAnswer": "A stack uses LIFO ordering..." }
  ]
}
```

**Validation:**

- Submission must be IN_PROGRESS
- Submission must not be expired
- Each `questionId` must belong to this quiz

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "savedAt": "2026-04-10T09:30:00Z",
    "answeredCount": 15,
    "totalQuestions": 20
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 CONFLICT

---

#### POST /api/quizzes/{quizId}/submissions/{submissionId}/submit (STUDENT)

Finalize and submit the attempt for grading. Auto-grading runs immediately for objective questions.

**Request:** None (empty body).

**Validation:**

- Submission must be IN_PROGRESS

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "SUBMITTED",
    "submittedAt": "2026-04-10T09:44:00Z",
    "autoGradedScore": 60,
    "pendingManualGrade": true
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Grading (4 endpoints, TA / INSTRUCTOR)

#### GET /api/grading/queue (TA, INSTRUCTOR)

List submissions awaiting manual grading.

**Query Parameters:** `page`, `size`, `quizId` (optional), `status` (optional: SUBMITTED, PARTIALLY_GRADED)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "submissionId": "uuid",
      "quizId": "uuid",
      "quizTitle": "Algorithms Quiz 3",
      "studentDisplayName": "Jane Doe",
      "attemptNumber": 1,
      "autoGradedScore": 60,
      "totalPoints": 100,
      "pendingQuestions": 3,
      "submittedAt": "2026-04-10T09:44:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 14, "totalPages": 1 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/grading/submissions/{submissionId} (TA, INSTRUCTOR)

Retrieve a submission for grading with all answers and rubrics.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "submissionId": "uuid",
    "quizTitle": "Algorithms Quiz 3",
    "studentDisplayName": "Jane Doe",
    "answers": [
      {
        "questionId": "uuid",
        "questionText": "Explain the difference between a stack and a queue.",
        "type": "SHORT_ANSWER",
        "textAnswer": "A stack is last-in first-out and a queue is first-in first-out.",
        "pointsPossible": 10,
        "pointsAwarded": null,
        "rubricItems": [
          { "id": "uuid", "criterion": "Mentions LIFO for stack", "points": 3, "satisfied": null },
          { "id": "uuid", "criterion": "Mentions FIFO for queue", "points": 3, "satisfied": null },
          { "id": "uuid", "criterion": "Provides a concrete example", "points": 4, "satisfied": null }
        ],
        "feedback": null
      }
    ]
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### PUT /api/grading/submissions/{submissionId}/answers/{questionId} (TA, INSTRUCTOR)

Grade a single answer. Rubric-based scoring is mandatory for SHORT_ANSWER and ESSAY types.

**Request:**

```json
{
  "pointsAwarded": 6,
  "rubricSelections": [
    { "rubricItemId": "uuid", "satisfied": true },
    { "rubricItemId": "uuid", "satisfied": true },
    { "rubricItemId": "uuid", "satisfied": false }
  ],
  "feedback": "Good explanation of LIFO and FIFO but no example provided."
}
```

**Validation:**

- `pointsAwarded` — required, integer 0 to pointsPossible
- `rubricSelections` — required for SHORT_ANSWER and ESSAY; must include all rubric items; sum of satisfied rubric points must equal `pointsAwarded`
- `feedback` — optional, max 2000 characters

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "questionId": "uuid",
    "pointsAwarded": 6,
    "feedback": "Good explanation of LIFO and FIFO but no example provided."
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/grading/submissions/{submissionId}/finalize (TA, INSTRUCTOR)

Finalize grading for a submission. All questions must be graded.

**Request:** None (empty body).

**Validation:**

- All questions must have `pointsAwarded` set.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "submissionId": "uuid",
    "status": "GRADED",
    "finalScore": 85,
    "totalPoints": 100,
    "gradedAt": "2026-04-11T14:00:00Z",
    "gradedBy": "uuid"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Schedules (6 endpoints, STUDENT)

#### GET /api/schedules (STUDENT)

List the current student's schedules (week plans).

**Query Parameters:** `page`, `size`, `sort`

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Spring 2026 Study Plan",
      "description": "Weekly study schedule for spring semester",
      "timezone": "America/New_York",
      "createdAt": "2026-01-20T10:00:00Z",
      "updatedAt": "2026-04-08T18:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 2, "totalPages": 1 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### GET /api/schedules/{id} (STUDENT)

Retrieve a single schedule with its entries.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Spring 2026 Study Plan",
    "description": "Weekly study schedule for spring semester",
    "timezone": "America/New_York",
    "entries": [
      {
        "id": "uuid",
        "dayOfWeek": "MONDAY",
        "startTime": "09:00",
        "endTime": "10:30",
        "title": "Algorithms Review",
        "category": "STUDY",
        "catalogItemId": "uuid",
        "notes": "Focus on graph algorithms"
      }
    ],
    "createdAt": "2026-01-20T10:00:00Z",
    "updatedAt": "2026-04-08T18:00:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/schedules (STUDENT)

Create a new schedule.

**Request:**

```json
{
  "name": "Summer Intensive Plan",
  "description": "Condensed study schedule for summer session",
  "timezone": "America/New_York"
}
```

**Validation:**

- `name` — required, 2-200 characters
- `description` — optional, max 1000 characters
- `timezone` — required, valid IANA timezone identifier

**Success Response (201):** Same shape as GET /api/schedules/{id} (with empty entries).

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### PUT /api/schedules/{id} (STUDENT)

Update schedule metadata.

**Request:** Same fields as POST, all optional.

**Success Response (200):** Same shape as GET /api/schedules/{id}.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### DELETE /api/schedules/{id} (STUDENT)

Delete a schedule and all its entries.

**Success Response (204):** No body.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/schedules/{id}/entries (STUDENT)

Add an entry to a schedule.

**Request:**

```json
{
  "dayOfWeek": "WEDNESDAY",
  "startTime": "14:00",
  "endTime": "15:30",
  "title": "Database Design Study",
  "category": "STUDY",
  "catalogItemId": "uuid",
  "notes": "ER diagrams and normalization"
}
```

**Validation:**

- `dayOfWeek` — required, one of: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
- `startTime` — required, HH:mm format, 00:00-23:59
- `endTime` — required, HH:mm format, must be after startTime
- `title` — required, 2-200 characters
- `category` — required, one of: STUDY, LECTURE, LAB, BREAK, OTHER
- `catalogItemId` — optional, valid catalog item UUID
- `notes` — optional, max 500 characters
- Must not overlap with existing entries on the same day (unless within a locked period, which returns LOCKED_PERIOD_CONFLICT)

**Success Response (201):** Single entry object.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 LOCKED_PERIOD_CONFLICT

---

### Timetable (6 endpoints, STUDENT)

#### GET /api/timetable (STUDENT)

Retrieve the student's active timetable view (weekly grid).

**Query Parameters:** `scheduleId` (required), `weekOf` (optional, ISO date for the Monday of the target week, defaults to current week)

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "scheduleId": "uuid",
    "weekOf": "2026-04-06",
    "timezone": "America/New_York",
    "slots": [
      {
        "id": "uuid",
        "dayOfWeek": "MONDAY",
        "startTime": "09:00",
        "endTime": "10:30",
        "title": "Algorithms Review",
        "category": "STUDY",
        "catalogItemId": "uuid",
        "locked": false,
        "row": 0,
        "col": 0,
        "rowSpan": 3
      }
    ]
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### PUT /api/timetable/slots/{slotId} (STUDENT)

Update a timetable slot (drag-and-drop move or resize). Represents the final state after a UI interaction.

**Request:**

```json
{
  "dayOfWeek": "TUESDAY",
  "startTime": "10:00",
  "endTime": "11:30"
}
```

**Validation:**

- `dayOfWeek` — required, valid day
- `startTime` — required, HH:mm format
- `endTime` — required, HH:mm format, must be after startTime
- Must not overlap with other slots
- Must not fall within a locked period

**Success Response (200):** Updated slot object.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 LOCKED_PERIOD_CONFLICT

---

#### POST /api/timetable/slots (STUDENT)

Add a new slot directly in the timetable view.

**Request:**

```json
{
  "scheduleId": "uuid",
  "dayOfWeek": "FRIDAY",
  "startTime": "13:00",
  "endTime": "14:00",
  "title": "Review Session",
  "category": "STUDY"
}
```

**Validation:** Same rules as schedule entry creation.

**Success Response (201):** Single slot object.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 409 LOCKED_PERIOD_CONFLICT

---

#### DELETE /api/timetable/slots/{slotId} (STUDENT)

Remove a slot from the timetable. Cannot remove slots in locked periods.

**Success Response (204):** No body.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 LOCKED_PERIOD_CONFLICT

---

#### POST /api/timetable/slots/{slotId}/split (STUDENT)

Split a session into two adjacent sessions at a given time.

**Request:**

```json
{
  "splitAt": "10:00"
}
```

**Validation:**

- `splitAt` — required, HH:mm, must be strictly between the slot's startTime and endTime
- Slot must not be locked

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "firstSlot": {
      "id": "uuid-new-1",
      "dayOfWeek": "MONDAY",
      "startTime": "09:00",
      "endTime": "10:00",
      "title": "Algorithms Review (1/2)",
      "category": "STUDY"
    },
    "secondSlot": {
      "id": "uuid-new-2",
      "dayOfWeek": "MONDAY",
      "startTime": "10:00",
      "endTime": "10:30",
      "title": "Algorithms Review (2/2)",
      "category": "STUDY"
    }
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 LOCKED_PERIOD_CONFLICT

---

#### POST /api/timetable/slots/merge (STUDENT)

Merge two adjacent slots on the same day into one.

**Request:**

```json
{
  "slotIds": ["uuid-1", "uuid-2"]
}
```

**Validation:**

- `slotIds` — required, exactly 2 UUIDs
- Both slots must be on the same day
- Slots must be adjacent (one's endTime equals the other's startTime)
- Neither slot may be locked

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid-merged",
    "dayOfWeek": "MONDAY",
    "startTime": "09:00",
    "endTime": "11:00",
    "title": "Algorithms Review",
    "category": "STUDY"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND, 409 LOCKED_PERIOD_CONFLICT

---

### Locked Periods (3 endpoints, STUDENT)

#### GET /api/schedules/{scheduleId}/locked-periods (STUDENT)

List locked periods for a schedule.

**Success Response (200):**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "scheduleId": "uuid",
      "dayOfWeek": "MONDAY",
      "startTime": "08:00",
      "endTime": "12:00",
      "reason": "Morning lectures - do not reschedule",
      "createdAt": "2026-03-01T10:00:00Z"
    }
  ]
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### POST /api/schedules/{scheduleId}/locked-periods (STUDENT)

Create a locked period. Any existing schedule entries that overlap will be preserved but become immovable.

**Request:**

```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "08:00",
  "endTime": "12:00",
  "reason": "Morning lectures - do not reschedule"
}
```

**Validation:**

- `dayOfWeek` — required, valid day
- `startTime` — required, HH:mm format
- `endTime` — required, HH:mm format, must be after startTime
- `reason` — optional, max 500 characters
- Locked periods may overlap existing entries; those entries become immovable

**Success Response (201):** Single locked period object.

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### DELETE /api/schedules/{scheduleId}/locked-periods/{periodId} (STUDENT)

Remove a locked period. Previously locked entries become editable again.

**Success Response (204):** No body.

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

### Plagiarism (3 endpoints, INSTRUCTOR / TA)

#### POST /api/plagiarism/checks (INSTRUCTOR, TA)

Initiate a plagiarism check on a set of submissions.

**Request:**

```json
{
  "quizId": "uuid",
  "submissionIds": ["uuid-1", "uuid-2", "uuid-3"],
  "algorithm": "COSINE_SIMILARITY",
  "threshold": 0.85
}
```

**Validation:**

- `quizId` — required, valid quiz UUID
- `submissionIds` — required, at least 2 submission UUIDs, all must belong to the specified quiz
- `algorithm` — required, one of: COSINE_SIMILARITY, JACCARD_INDEX, LEVENSHTEIN_RATIO, N_GRAM_OVERLAP
- `threshold` — required, decimal 0.0-1.0, similarity score above which pairs are flagged

**Success Response (201):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "quizId": "uuid",
    "status": "PENDING",
    "algorithm": "COSINE_SIMILARITY",
    "threshold": 0.85,
    "submissionCount": 3,
    "createdAt": "2026-04-11T15:00:00Z"
  }
}
```

**Error Codes:** 400 VALIDATION_ERROR, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### GET /api/plagiarism/checks/{checkId} (INSTRUCTOR, TA)

Retrieve plagiarism check results.

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "quizId": "uuid",
    "status": "COMPLETED",
    "algorithm": "COSINE_SIMILARITY",
    "threshold": 0.85,
    "submissionCount": 3,
    "flaggedPairs": [
      {
        "submissionA": { "id": "uuid-1", "studentDisplayName": "Alice" },
        "submissionB": { "id": "uuid-2", "studentDisplayName": "Bob" },
        "similarityScore": 0.92,
        "matchedFragments": [
          {
            "textA": "A stack uses last-in first-out ordering...",
            "textB": "A stack uses last-in, first-out ordering...",
            "questionId": "uuid"
          }
        ]
      }
    ],
    "completedAt": "2026-04-11T15:02:00Z",
    "createdAt": "2026-04-11T15:00:00Z"
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND

---

#### GET /api/plagiarism/checks (INSTRUCTOR, TA)

List plagiarism checks with pagination.

**Query Parameters:** `page`, `size`, `quizId` (optional), `status` (optional: PENDING, RUNNING, COMPLETED, FAILED)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "quizId": "uuid",
      "quizTitle": "Algorithms Quiz 3",
      "status": "COMPLETED",
      "algorithm": "COSINE_SIMILARITY",
      "threshold": 0.85,
      "submissionCount": 25,
      "flaggedPairCount": 2,
      "completedAt": "2026-04-11T15:02:00Z",
      "createdAt": "2026-04-11T15:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 4, "totalPages": 1 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

### Wrong Answers (2 endpoints, STUDENT)

#### GET /api/wrong-answers (STUDENT)

List the current student's wrong answers across all quizzes for review.

**Query Parameters:** `page`, `size`, `sort`, `quizId` (optional), `tagId` (optional)

**Success Response (200) — Paged:**

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "submissionId": "uuid",
      "quizId": "uuid",
      "quizTitle": "Data Structures Midterm",
      "questionId": "uuid",
      "questionText": "What is the time complexity of binary search?",
      "questionType": "MULTIPLE_CHOICE",
      "studentAnswer": "O(n)",
      "correctAnswer": "O(log n)",
      "pointsAwarded": 0,
      "pointsPossible": 5,
      "explanation": "Binary search halves the search space each step.",
      "tags": [{ "id": "uuid", "name": "searching" }],
      "reviewedAt": null,
      "createdAt": "2026-04-11T14:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 8, "totalPages": 1 }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN

---

#### PUT /api/wrong-answers/{id}/review (STUDENT)

Mark a wrong answer as reviewed.

**Request:**

```json
{
  "notes": "I confused binary search with linear search. Remember: binary search requires sorted input and divides in half."
}
```

**Validation:**

- `notes` — optional, max 2000 characters

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "reviewedAt": "2026-04-12T10:00:00Z",
    "notes": "I confused binary search with linear search. Remember: binary search requires sorted input and divides in half."
  }
}
```

**Error Codes:** 401 UNAUTHORIZED, 403 FORBIDDEN, 404 RESOURCE_NOT_FOUND
