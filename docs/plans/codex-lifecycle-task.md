# Slice 3 - Tasks + Lifecycle Implementation Task

## Status
Complete. Backend/frontend automated verification and manual lifecycle smoke passed after the custom DateTimePicker and UI redesign changes.

## Goal
Complete the single-user Task management loop without introducing Course, effort, importance, notes, authentication, ownership, or hard delete.

## Backend Scope
- Keep `TaskStatus` limited to `ACTIVE` and `COMPLETED`.
- Preserve the database invariant:
  - ACTIVE -> `completedAt == null`
  - COMPLETED -> `completedAt != null`
- Add domain lifecycle behavior:
  - edit title + dueAt
  - complete
  - reopen
- Complete and reopen are idempotent.
- Completed Tasks are never considered overdue and remain excluded from scheduling.
- Add completed Task listing ordered by `completedAt` descending with stable ID ordering.
- Add APIs:
  - `GET /api/tasks?status=ACTIVE|COMPLETED`
  - `GET /api/tasks/{taskId}`
  - `PUT /api/tasks/{taskId}` with title + dueAt only
  - `POST /api/tasks/{taskId}/complete`
  - `POST /api/tasks/{taskId}/reopen`
- Add `completedAt` to the current Task response.

## Frontend Scope
- Add `/tasks` route.
- Add `/tasks/:taskId` route.
- Add Today/Tasks primary navigation.
- Add Active / Completed tabs.
- Add complete/reopen controls.
- Add Task Detail editing for title + due date/time only.
- Keep Quick Add unchanged.
- Invalidate only relevant Today, Task list, and Task Detail queries after lifecycle mutations.
- Completed Tasks show completion state instead of overdue feedback.

## Explicit Non-Goals
Do not add:
- Course
- estimatedMinutes
- importance
- notes
- risk enrichment
- authentication/session/ownership
- hard delete
- AI

## Verification Status
- [x] Backend: `mvn test`
- [x] Frontend: `npm run typecheck`
- [x] Frontend: `npm run lint`
- [x] Frontend: `npm test`
- [x] Frontend: `npm run build`
- [x] Manual lifecycle smoke

Manual smoke:
```text
open Tasks
-> create or select active task
-> edit title/deadline
-> complete
-> verify task leaves Active and Today recommendation
-> open Completed
-> reopen
-> verify task returns to Active and Today consideration
-> refresh and confirm persistence
```

## Done When
- automated backend/frontend verification passes
- manual lifecycle smoke passes
- no Slice 4/5/6 fields or behavior leaked into the implementation
- docs and API contract match the implemented Slice 3 behavior
