# Slice 5 - Courses Implementation Task

## Status
Complete. Automated verification and manual Course smoke passed.

## Goal
Add optional Course organization without slowing Quick Add or changing deterministic scheduling behavior.

## Backend Scope
- Add Flyway V3 with `courses` and nullable `tasks.course_id`.
- Use a foreign key with `ON DELETE SET NULL` so removing a Course never removes Tasks.
- Add Course create/list/rename/remove APIs.
- Keep the current single-user trial; ownership remains deferred to Slice 6.
- Expose a narrow `CourseLookup` application contract to Task.
- Allow optional `courseId` on Task create/update and reject unknown Course IDs.
- Add optional `courseId` filtering to Task lists.
- Return Course summary `{ id, name }` with Task responses.
- Pass Course context through Today display data without using Course in scheduling score, risk, urgency, or tie-breaking.

## Frontend Scope
- Add an optional CourseSelector to Quick Add and Task Detail.
- Add a lightweight inline CourseManager on Tasks for create/rename/remove.
- Add Course filter to Active and Completed Tasks.
- Show Course chips on task cards and Today recommendation.
- Keep Course loading/errors non-blocking for task capture.
- Require an explicit second action before removing a Course.

## Explicit Non-Goals
Do not add:
- authentication or ownership
- Course-based scheduling weights
- Course deletion of Tasks
- Classroom/Teams integration
- hard Task delete
- AI

## Verification
- [x] Backend: `mvn test`
- [x] Frontend: `npm run typecheck`
- [x] Frontend: `npm run lint`
- [x] Frontend: `npm test`
- [x] Frontend: `npm run build`
- [x] `git diff --check`
- [x] Manual Course smoke

## Manual Smoke
```text
open Tasks
-> Manage courses
-> create a Course
-> Quick Add a Task with that Course
-> confirm the Task and Today recommendation show the Course
-> open Task Detail and change or clear Course
-> filter Tasks by Course
-> rename a Course and confirm labels update
-> remove a Course with confirmation
-> confirm linked Tasks remain and become unassigned
-> refresh and confirm persistence
```

## Done When
- Course remains optional in Quick Add and Task Detail
- Course CRUD persists correctly
- Task Course assignment can be added, changed, or cleared
- filtering works for active/completed Tasks
- Course removal preserves Tasks and clears their association
- scheduling behavior is unchanged by Course
- automated and manual verification pass
- no Slice 6 authentication/ownership behavior leaks into the implementation
