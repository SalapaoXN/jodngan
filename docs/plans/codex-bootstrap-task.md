# Codex Task: Bootstrap JodNgan Foundation

Use this task when handing Slice 0 to Codex CLI.

---

## Instructions

Read first:
- `AGENTS.md`
- `PLANS.md`
- `docs/plans/bootstrap-spec.md`
- `docs/plans/vertical-slices.md`
- `docs/architecture/domain-model.md`
- `docs/architecture/database.md`
- `docs/architecture/api-contract.md`
- `docs/architecture/module-boundaries.md`
- `docs/architecture/frontend-structure.md`
- `docs/architecture/testing-strategy.md`
- `docs/architecture/decisions/0001-modular-monolith.md`
- `docs/architecture/decisions/0002-session-authentication.md`

Review, finish, and verify **Slice 0 - Foundation only**.

A draft Slice 0 scaffold already exists in `backend/`, `frontend/`, `compose.yml`, and `.env.example`. Inspect it first; fix compatibility/build issues instead of regenerating the project blindly.

Do not start Authentication or Task features yet.

### Backend
Bootstrap:
- Java 21
- Maven
- Spring Boot 4.1.2
- package `com.jodngan`

Add only foundation dependencies required by `bootstrap-spec.md`.

Configure:
- Spring Web
- Validation
- Security foundation only; do not implement auth feature yet
- Spring Data JPA
- PostgreSQL
- Flyway
- test foundation
- injectable `Clock`
- consistent error-response foundation where useful

Do not create empty placeholder domain classes.

The backend smoke context test uses PostgreSQL Testcontainers and has been verified successfully with Maven and Docker. Slice 0 is closed; this file remains as the record of the bootstrap task.

### Frontend
Bootstrap:
- Node 24 LTS-compatible project
- Vite 8.3 stable line
- React 19.3
- TypeScript
- React Router
- TanStack Query
- React Hook Form

Create only the minimal app/provider/router/API-client structure required for Slice 0.

Do not implement real product screens beyond a minimal integration/smoke shell.

### Local development
Add PostgreSQL Docker Compose configuration.

Use environment variables and example configuration only.
Never commit real credentials.

Configure Vite to proxy `/api` to the Spring backend in development.

### Verification
Run all relevant commands available in the environment, including:
- backend compile/tests
- frontend install
- frontend typecheck
- frontend lint
- frontend tests if configured
- frontend production build
- database/Flyway startup verification where practical

Fix failures before declaring Slice 0 complete.

Inspect the final Git diff and remove unrelated/generated files that should not be committed.

Do not stage, commit, or push unless explicitly asked.

### Final report
Report only:
1. files/structure created
2. dependencies/tooling chosen
3. commands run and results
4. any unresolved issue blocking Slice 1

If a design choice conflicts with existing docs, stop and report the conflict instead of silently changing the agreed architecture.
