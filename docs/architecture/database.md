# Database Design

## Status
Multi-user ownership design is implemented in Slice 6. PostgreSQL now stores Users and requires `user_id` ownership on Tasks and Courses.

## Database
PostgreSQL is the system of record.

Schema changes will be managed with Flyway migrations once implementation begins.

---

# Design Principles

- Persist source-of-truth data, not time-sensitive derived values.
- Keep explicit User ownership on Task/Course data; the Slice 6 migration did not invent a fake User for earlier unowned trial rows.
- Use database constraints for structural integrity.
- Keep scheduling rules in the application/domain layer.
- Store timestamps as `timestamptz` / Java `Instant`.

---

# Historical Single-User Trial Schema

Slices 1-5 persisted Tasks/Courses without a User boundary to validate the core planning value before authentication.

Initial `tasks` fields:

| Column | Type | Null | Notes |
| --- | --- | --- | --- |
| id | uuid | no | primary key |
| title | varchar(200) | no | trimmed task title |
| due_at | timestamptz | no | absolute deadline |
| status | varchar(16) | no | ACTIVE / COMPLETED |
| completed_at | timestamptz | yes | used once lifecycle is implemented |
| created_at | timestamptz | no | creation instant |
| updated_at | timestamptz | no | last update instant |

Slice 1 may create only the fields it actually uses, but migrations remain append-only once committed. `estimated_minutes`, `importance`, `notes`, and `course_id` are added by later core slices when required.

Slice 6 supersedes this historical schema with V4 ownership. Because the private trial rows had no trustworthy owner mapping, V4 intentionally clears existing trial Tasks/Courses before adding required ownership. It does not create a hidden fake User.

---

# Current Multi-User Tables (Slice 6)

## users

| Column | Type | Null | Notes |
| --- | --- | --- | --- |
| id | uuid | no | primary key |
| email | varchar(320) | no | normalized lowercase email |
| password_hash | varchar(255) | no | encoded password only |
| time_zone | varchar(64) | no | valid IANA time-zone id |
| created_at | timestamptz | no | creation instant |
| updated_at | timestamptz | no | last update instant |

Constraints:
- primary key `(id)`
- unique `(email)`

The application normalizes email before persistence.

---

## courses

| Column | Type | Null | Notes |
| --- | --- | --- | --- |
| id | uuid | no | primary key |
| user_id | uuid | no | owner |
| name | varchar(120) | no | trimmed course name |
| created_at | timestamptz | no | creation instant |
| updated_at | timestamptz | no | last update instant |

Constraints:
- primary key `(id)`
- foreign key `user_id -> users.id ON DELETE CASCADE`
- `name <> ''` after application normalization

Recommended index:
- `(user_id, name)`

Duplicate course names are allowed in the first MVP because course naming conventions vary and duplicate prevention is not a product requirement.

---

## tasks

| Column | Type | Null | Notes |
| --- | --- | --- | --- |
| id | uuid | no | primary key |
| user_id | uuid | no | owner |
| course_id | uuid | yes | optional Course |
| title | varchar(200) | no | trimmed task title |
| due_at | timestamptz | no | absolute deadline |
| estimated_minutes | integer | yes | progressive planning data |
| importance | varchar(16) | yes | LOW / NORMAL / HIGH |
| notes | text | yes | optional notes |
| status | varchar(16) | no | ACTIVE / COMPLETED |
| completed_at | timestamptz | yes | set only when completed |
| created_at | timestamptz | no | creation instant |
| updated_at | timestamptz | no | last update instant |

Constraints:
- primary key `(id)`
- foreign key `user_id -> users.id ON DELETE CASCADE`
- foreign key `course_id -> courses.id ON DELETE SET NULL`
- `estimated_minutes > 0` when non-null
- `importance IN ('LOW', 'NORMAL', 'HIGH')` when non-null
- `status IN ('ACTIVE', 'COMPLETED')`
- status/completion consistency:
  - ACTIVE -> `completed_at IS NULL`
  - COMPLETED -> `completed_at IS NOT NULL`

Recommended indexes:
- `(user_id, status, due_at)` for Today and active/completed lists
- `(user_id, course_id, status)` for Course filtering

---

# Relationship Behavior

```text
users
  ├──< courses
  └──< tasks >── optional course
```

## Delete User
If account deletion is added later, owned Courses and Tasks may cascade with the User.

There is no user-delete endpoint required for the first MVP.

## Delete Course
`course_id` becomes null on linked Tasks.

Tasks remain intact.

## Delete Task
No hard-delete operation exists in the first MVP.

---

# Ownership Integrity

The database stores `user_id` on both Course and Task.

The application must validate that any Course assigned to a Task belongs to the same authenticated User.

Repository/application queries for user-owned resources must include ownership, conceptually:

```text
find task where id = :taskId and user_id = :authenticatedUserId
```

Do not load a Task by ID and rely on the frontend to enforce ownership.

---

# Persisted vs Derived

Persist:
- Task source fields
- lifecycle status
- completion timestamp
- ownership

Do not persist initially:
- remaining time
- due-today flag
- overdue flag
- urgency band
- recommendation score
- recommendation reason
- start guidance
- risk status

Those values depend on current time and must be recalculated.

---

# Time Handling

## Backend
Use `Instant` for persisted timestamps.

## Frontend
The user selects a local date/time. The frontend converts it to an absolute timestamp using the browser/user time zone before sending it to the API.

## User Time Zone
Registration detects the browser IANA time-zone identifier and stores it on `users.time_zone`. Persisted deadlines remain absolute instants; the User time zone is account metadata for local-calendar semantics.

Do not store server-local timestamps without an offset/instant interpretation.

---

# Migration Plan

Current append-only migration sequence:

```text
V1__create_tasks.sql
V2__add_task_planning_fields.sql
V3__add_courses.sql
V4__add_users_and_ownership.sql
```

V4 creates `users`, adds required `user_id` ownership to Courses/Tasks, adds ownership-oriented indexes, and intentionally resets the earlier private trial rows because no trustworthy owner mapping exists.

During implementation, migrations are append-only after they have been shared/merged. Do not silently rewrite applied migrations.

---

# Current Multi-User Query Patterns

All Task/Course persistence access is ownership-scoped in Slice 6. Cross-user lookups return no owned resource rather than loading globally and checking later.

## Today

```text
active Tasks for user
ordered/processed by scheduling engine
```

Repository candidate:

```text
WHERE user_id = ?
  AND status = 'ACTIVE'
ORDER BY due_at ASC
```

The final recommendation order is domain logic, not SQL priority logic.

## Tasks - Active

```text
WHERE user_id = ? AND status = 'ACTIVE'
ORDER BY due_at ASC
```

## Tasks - Completed

```text
WHERE user_id = ? AND status = 'COMPLETED'
ORDER BY completed_at DESC
```

## Filter by Course
Add:

```text
AND course_id = ?
```

with ownership validated against the authenticated user.

---

# ER Diagram

```text
┌────────────────────┐
│ users              │
├────────────────────┤
│ PK id              │
│ UQ email           │
│ password_hash      │
│ time_zone          │
│ created_at         │
│ updated_at         │
└─────────┬──────────┘
          │ 1
      ┌───┴───────────────┐
      │                   │
      │ *                 │ *
┌─────▼──────────────┐  ┌─▼──────────────────┐
│ courses            │  │ tasks              │
├────────────────────┤  ├────────────────────┤
│ PK id              │  │ PK id              │
│ FK user_id         │  │ FK user_id         │
│ name               │  │ FK course_id NULL  │
│ created_at         │  │ title              │
│ updated_at         │  │ due_at             │
└─────────┬──────────┘  │ estimated_minutes  │
          │ 0..1        │ importance NULL    │
          └─────────────│ notes NULL         │
                        │ status             │
                        │ completed_at NULL  │
                        │ created_at         │
                        │ updated_at         │
                        └────────────────────┘
```

---

# Schema Acceptance Checklist

- [x] User ownership is explicit.
- [x] Course is optional for Task.
- [x] Course deletion preserves Tasks.
- [x] Effort and importance remain optional.
- [x] Task lifecycle consistency can be constrained.
- [x] Today/Tasks queries have direct indexes.
- [x] Derived planning values are not persisted.
- [x] Schema supports deterministic recalculation as time changes.
