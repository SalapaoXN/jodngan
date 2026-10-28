# Codex Task: Implement Slice 1 Core Capture

## Status
Complete. The single-user Quick Add flow was manually verified end to end with PostgreSQL persistence and refresh survival. Backend integration tests passed 9/9 before final cleanup; frontend typecheck, lint, 7 tests, and production build passed. The retained foundation smoke test should remain part of normal Maven verification.

## Goal
Implement JodNgan Slice 1 as a real single-user trial flow: open the app, add a Task quickly, persist it in PostgreSQL, and immediately see how much time remains until the deadline.

This slice exists to validate the core product pain point before adding account/authentication complexity.

Do not implement authentication, registration, login, logout, User persistence, ownership, Course, scheduling recommendation, effort, importance, notes, notifications, AI, or calendar integrations in this task.

---

## Read First
- `AGENTS.md`
- `PLANS.md`
- `docs/product/problem.md`
- `docs/product/quick-capture-principle.md`
- `docs/plans/vertical-slices.md`
- `docs/architecture/domain-model.md`
- `docs/architecture/database.md`
- `docs/architecture/api-contract.md`
- `docs/architecture/module-boundaries.md`
- `docs/architecture/frontend-structure.md`
- `docs/architecture/testing-strategy.md`

Slice 0 foundation is complete and verified.

Current mode is intentionally **single-user trial**. Do not create a fake User or implement the deferred authentication design.

---

# Backend

## 1. First Flyway Migration
Create:

```text
backend/src/main/resources/db/migration/V1__create_tasks.sql
```

Create only the Task schema required for this trial slice:

```text
id uuid primary key
title varchar(200) not null
due_at timestamptz not null
status varchar(16) not null
completed_at timestamptz null
created_at timestamptz not null
updated_at timestamptz not null
```

Constraints:
- `status IN ('ACTIVE', 'COMPLETED')`
- `ACTIVE -> completed_at IS NULL`
- `COMPLETED -> completed_at IS NOT NULL`

Slice 1 creates Tasks only as `ACTIVE`; the lifecycle actions come later.

Do not create:
- `users`
- `courses`
- `user_id`
- `course_id`
- `estimated_minutes`
- `importance`
- `notes`

Do not reserve migration numbers for deferred authentication.

## 2. Task Module
Implement the smallest coherent Task vertical slice under:

```text
com.jodngan.task
├── api
├── application
├── domain
└── infrastructure
```

Required behavior:
- create Task
- list active Tasks ordered by `dueAt ASC`, then stable ID ordering if needed
- get one Task by ID
- calculate remaining time from `dueAt` and the injected `Clock`
- identify overdue Tasks

Keep controllers thin. Business/time calculations do not belong in controllers.

Use the existing injectable `Clock` rather than calling `Instant.now()` throughout the code.

## 3. Task API
For this slice implement:

```text
POST /api/tasks
GET  /api/tasks
GET  /api/tasks/{taskId}
```

### POST /api/tasks
Request:

```json
{
  "title": "Database Assignment",
  "dueAt": "2026-10-31T16:59:00Z"
}
```

Validation:
- title required after trim
- title max 200 chars
- dueAt required
- past deadlines are allowed

Response: `201` with the created Task and planning data needed by the trial UI.

A compact response shape is acceptable, for example:

```json
{
  "id": "uuid",
  "title": "Database Assignment",
  "dueAt": "2026-10-31T16:59:00Z",
  "status": "ACTIVE",
  "createdAt": "2026-10-29T00:00:00Z",
  "updatedAt": "2026-10-29T00:00:00Z",
  "planning": {
    "remainingSeconds": 120000,
    "overdue": false
  }
}
```

Do not persist `remainingSeconds` or `overdue`.

### GET /api/tasks
Return active Tasks ordered by earliest deadline first.

Response:

```json
{
  "items": []
}
```

Each item includes the same deadline/planning data needed by the frontend.

### GET /api/tasks/{taskId}
Return one Task or `404 RESOURCE_NOT_FOUND`.

No ownership lookup exists in this single-user slice.

## 4. Error Handling
Use the existing accepted error envelope where practical.

At minimum:
- `400 VALIDATION_ERROR`
- `404 RESOURCE_NOT_FOUND`
- `500 INTERNAL_ERROR`

Do not expose SQL/framework stack details.

## 5. Temporary Security Configuration
The current foundation security config requires authentication for every non-health request. That conflicts with the agreed single-user trial.

For the trial only:
- allow the core `/api/**` application endpoints without authentication
- no login/session is required
- disable/ignore CSRF for this unauthenticated trial API so Quick Add POST works without introducing the deferred auth token flow

Keep the change obvious and easy to reverse in Slice 6 Authentication. A short code comment is acceptable.

Do not add JWT, session login, CSRF token endpoint, or fake credentials.

This trial configuration must not be presented as production-ready security and must not be exposed directly to the public internet.

---

# Frontend

## 1. Replace Foundation Smoke Screen
Replace the temporary health-only screen with the first real product screen.

The app should open directly at `/` with no auth redirect.

Build a minimal Today/capture surface that preserves the Figma/product hierarchy without spending this slice on final visual polish.

Required states:
- loading
- empty
- list with Tasks
- recoverable error

## 2. Quick Add
Implement a lightweight Quick Add interaction.

Required fields only:
1. Task name
2. Due date/time
3. Save

Do not add:
- Course
- estimated effort
- importance
- notes

The capture flow should feel close to writing a short note.

Use `datetime-local` or an equally simple browser-native control. Interpret it in the browser local time zone and send an absolute ISO timestamp to the backend.

## 3. Task Display
After save:
- close/reset Quick Add as appropriate
- make the created Task appear immediately
- refresh/invalidate the active Task query
- show title
- show deadline in local time
- show remaining/overdue feedback

Frontend may format a human-readable label from backend `remainingSeconds` / `overdue`, but must not invent recommendation ranking yet.

Examples:
- `เหลือ 5 ชั่วโมง`
- `เหลือ 2 วัน`
- `เลยกำหนดแล้ว 3 ชั่วโมง`

Keep formatting simple and deterministic; do not build a full localization framework in this slice.

## 4. Frontend Structure
Use the existing stack:
- React
- TypeScript
- TanStack Query
- React Hook Form where useful
- shared API client

Suggested feature area:

```text
src/features/tasks/
├── api.ts
├── queries.ts
├── types.ts
├── QuickAddTask.tsx
├── TaskListItem.tsx
└── TodayPage.tsx
```

Do not create SignInPage, RegisterPage, AuthGuard, auth query state, or CSRF client state.

---

# Tests

## Backend
Required behavior coverage:
1. Flyway V1 applies to empty PostgreSQL.
2. create Task with title + future dueAt.
3. title is trimmed before persistence.
4. blank title rejected.
5. title over 200 rejected.
6. missing dueAt rejected.
7. past dueAt is accepted.
8. future Task returns positive remaining time.
9. past Task returns overdue true.
10. active list ordered by deadline ascending.
11. missing Task ID returns 404 contract.
12. time-dependent tests use fixed `Clock`/Instant.

Use PostgreSQL/Testcontainers for persistence behavior where appropriate.

## Frontend
At minimum cover:
- Today empty state
- opening Quick Add
- required field feedback
- successful submit sends title + absolute dueAt only
- created/listed Task is rendered
- remaining/overdue label renders
- server error is visible and recoverable

Test user-visible behavior; do not test CSS implementation details.

---

# Verification
Run and fix all relevant checks available in the environment.

Backend:

```text
mvn test
```

Frontend:

```text
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

Also inspect:

```text
git diff --check
git status
```

Where practical, manually verify the development flow:

```text
open JodNgan
→ no login screen
→ add "Database Assignment"
→ choose deadline
→ save
→ task appears
→ remaining/overdue information is visible
→ refresh browser
→ task is still present from PostgreSQL
```

Do not stage, commit, or push.

---

# Definition of Done
Slice 1 is complete only when:
- JodNgan opens without authentication
- PostgreSQL persists a real Task
- Quick Add requires only title + deadline
- refresh keeps the Task
- remaining time / overdue state is derived from the current clock
- backend tests pass
- frontend typecheck/lint/tests/build pass
- no User/Auth/Course/Scheduling implementation has leaked into this slice

---

# Final Report
Report only:
1. files/features implemented
2. migration/API approach
3. backend/frontend verification results
4. manual trial-flow result if run
5. unresolved blocker, if any

If existing code conflicts with the updated single-user trial docs, follow the updated docs and report the conflict instead of reintroducing authentication.
