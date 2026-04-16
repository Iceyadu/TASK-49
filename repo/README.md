# Project Type: fullstack

# ScholarOps Offline Learning and Content Intake

ScholarOps is a role-based fullstack system with:
- Spring Boot backend (`backend/`)
- Vue 3 frontend (`frontend/`)
- MySQL + app stack via Docker Compose (`docker-compose.yml`)

## Quick Start (Docker, full stack)

From `repo/`:

```bash
docker compose up -d
# or: docker-compose up -d
```

This starts:
- `mysql` on `3306`
- `backend` on `8080`
- `frontend` on `5173`

## Access URLs

- Frontend: `http://localhost:5173/login`
- Backend base URL: `http://localhost:8080`
- Backend login endpoint: `http://localhost:8080/api/auth/login`

## Demo Credentials (seeded by Flyway)

All accounts are created automatically by DB migrations.

- `ADMINISTRATOR`
  - username: `admin`
  - password: `Admin@12345678`
- `CONTENT_CURATOR`
  - username: `curator.integration`
  - password: `Curator@12345`
- `INSTRUCTOR`
  - username: `instructor.quiz.api`
  - password: `Instructor@12345`
- `TEACHING_ASSISTANT`
  - username: `ta.integration`
  - password: `Ta@12345`
- `STUDENT`
  - username: `student.integration`
  - password: `Student@12345`

## Verify the System

### 1) Health/auth verification with curl

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@12345678"}'
```

Expected: HTTP `200` with `accessToken` and `refreshToken` in JSON.

### 2) UI verification flow

1. Open `http://localhost:5173/login`
2. Sign in as `admin` / `Admin@12345678`
3. Confirm you land on dashboard (`/`) and can open admin pages such as `/admin/users`

## Tests

Test folders:
- `unit_tests/` - frontend Vitest specs
- `e2e/` - Playwright browser + real-network API specs
- `api_tests/` - reserved API test assets (backend API suites run via Maven)

Main runner from `repo/`:

```bash
./run_tests.sh all
```

Selective suites:
- `./run_tests.sh backend`
- `./run_tests.sh frontend`
- `./run_tests.sh api`
- `./run_tests.sh e2e`

## Project Structure

- `backend/` - Spring Boot API, security, crawler, grading, schedule, plagiarism, Flyway
- `frontend/` - Vue app
- `unit_tests/` - frontend unit specs
- `e2e/` - Playwright specs + API helpers
- `api_tests/` - API test folder placeholder
- `docker-compose.yml` - local fullstack stack

## Stop Services

From `repo/`:

```bash
docker compose down
# or: docker-compose down
```
