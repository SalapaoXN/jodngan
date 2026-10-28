# Database Design

## Status
Target multi-user design accepted. Current implementation starts in single-user trial mode and defers User ownership until the authentication slice.

## Database
PostgreSQL is the system of record.

Schema changes will be managed with Flyway migrations once implementation begins.

---

# Design Principles

- Persist source-of-truth data, not time-sensitive derived values.
- Add explicit ownership when authentication is introduced; do not invent a fake User solely to unblock the single-user trial.
- Use database constraints for structural integrity.
- Keep scheduling rules in the application/domain layer.
- Store timestamps as `timestamptz` / Java `Instant`.

---

# Current Single-User Trial Schema

The first usable build persists Tasks without a User/account boundary. This keeps capture and recommendation work independent from authentication.

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

No `users` table and no `user_id` column are required before the authentication slice.

Trial data preservation when authentication is added will be decided at that time. For a small private trial, resetting data is acceptable unless preserving it becomes a real requirement; otherwise implement an explicit claim/backfill migration rather than a hidden fake account.

---

# Target Multi-User Tables (After Authentication)

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
During the single-user trial, the frontend uses the browser's local time zone for local-calendar display while deadlines are persisted as absolute instants.

When authentication is introduced, store an IANA identifier on `users.time_zone` for authoritative per-user local-calendar labels such as "due today".

Do not store server-local timestamps without an offset/instant interpretation.

---

# Migration Plan

Current single-user sequence begins with the core product rather than accounts:

```text
V1__create_tasks.sql
```

Later core migrations add Task planning fields and Courses only when their slices require them.

Authentication is implemented later with new append-only migrations that create `users` and add ownership to existing Task/Course data. Do not reserve or rewrite old migration numbers merely to match the earlier design draft.

During implementation, migrations are append-only after they have been shared/merged. Do not silently rewrite applied migrations.

---

# Target Multi-User Query Patterns

The following ownership-scoped patterns apply after authentication is introduced. Before that, the single-user trial queries Tasks directly without `user_id` predicates.

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
