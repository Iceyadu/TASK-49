# ScholarOps Offline Learning & Content Intake

## Project Structure
- `backend/`: Spring Boot API, security, crawler, grading, schedule, plagiarism, Flyway schema.
- `frontend/`: Vue 3 + Vite web app with role-specific workspaces.
- `docker-compose.yml`: local stack for MySQL + backend + frontend containers.

## Local Run (without Docker)
### Backend
1. Start MySQL 8 and create database `scholarops`.
2. Configure env vars (or rely on defaults in `application.yml`):
   - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
   - `JWT_SECRET`
   - `SCHOLAROPS_AES_KEY` (32-byte key)
3. Run:
   - `cd backend`
   - `./mvnw spring-boot:run` (or `mvn spring-boot:run`)

### Frontend
1. Configure API base URL if needed in frontend env config.
2. Run:
   - `cd frontend`
   - `npm install`
   - `npm run dev`

## Docker Run
From `repo/`:

```bash
docker compose up --build
```

- **API:** http://localhost:8080  
- **UI:** http://localhost:8088 (default; avoids host port 80 conflicts)

Optional: `FRONTEND_PORT=3000 docker compose up --build` to map the UI to another host port.

Images are built from `docker/Dockerfile.backend` (Maven) and `docker/Dockerfile.frontend` (npm + nginx), so you do **not** need a prebuilt JAR or `frontend/dist` on the host.

## Tests
### Backend
- `cd backend`
- `./mvnw test` (or `mvn test`)

### Frontend
- `cd frontend`
- `npm test`

## Security Notes
- Passwords are validated by policy and hashed with BCrypt.
- Crawl source credentials are encrypted using AES at rest.
- JWT auth protects `/api/**` except login/refresh endpoints.

