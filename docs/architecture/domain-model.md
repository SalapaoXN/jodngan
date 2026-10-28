# Domain Model

## Status
Target domain model accepted. Current implementation begins with a single-user Task-only trial and adds User ownership later.

## Goal
Define the smallest domain model that supports fast task capture, course organization, task lifecycle, and deterministic planning without persisting values that can be recalculated.

## Core Model

```text
User 1 ─────── * Course
  │
  └─────────── * Task
                  │
                  └──── 0..1 Course

Task + Current Time
        │
        v
Scheduling Engine
        │
        ├─ Remaining Time
        ├─ Urgency
        ├─ Recommendation
        └─ Risk Assessment
```

The scheduling outputs are derived values, not entities.

## Current Trial Simplification
Before authentication exists, JodNgan is intentionally single-user:
- Task does not require `userId` yet
- Course is deferred until its core slice
- initial Task capture requires only `title` and `dueAt`
- effort, importance, and notes are added progressively
- ownership rules below become active when the authentication slice is implemented

Do not create a fake User solely to satisfy the future multi-user model.

---

# User (Deferred Until Authentication)

## Responsibility
- Represents the authenticated account.
- Defines the ownership boundary for Courses and Tasks.
- Provides the user's time zone for deadline interpretation.

## Persisted Fields
- `id: UUID`
- `email: String`
- `passwordHash: String`
- `timeZone: String` — IANA time-zone identifier such as `Asia/Bangkok` or `Europe/London`
- `createdAt: Instant`
- `updatedAt: Instant`

## Invariants
- Email is required.
- Email is normalized before persistence.
- Email is unique after normalization.
- Password hash is required; plaintext passwords are never stored.
- Time zone must be a valid IANA zone identifier.

## Notes
The time zone does not need to be a visible registration field. The frontend may detect the browser time zone and send it during account creation. It exists so the backend can interpret concepts such as "due today" consistently.

---

# Course

## Responsibility
Groups Tasks by academic course without becoming mandatory for task capture.

## Persisted Fields
- `id: UUID`
- `userId: UUID`
- `name: String`
- `createdAt: Instant`
- `updatedAt: Instant`

## Invariants
- Course belongs to exactly one User.
- Name is required after trimming.
- A Course cannot be assigned to another user's Task.

## Removal Rule
Removing a Course never deletes its Tasks.

When a Course is removed:
- Tasks that referenced it become unassigned (`courseId = null`).
- Other Task data remains unchanged.

---

# Task

## Responsibility
Represents one piece of student work that may be progressively enriched after initial capture.

## Persisted Fields
- `id: UUID`
- `userId: UUID`
- `courseId: UUID?`
- `title: String`
- `dueAt: Instant`
- `estimatedMinutes: Integer?`
- `importance: Importance?`
- `notes: String?`
- `status: TaskStatus`
- `completedAt: Instant?`
- `createdAt: Instant`
- `updatedAt: Instant`

## TaskStatus

```text
ACTIVE
COMPLETED
```

Lifecycle:

```text
ACTIVE ── complete() ──> COMPLETED
   ^                       │
   └────── reopen() ───────┘
```

## Importance

```text
LOW
NORMAL
HIGH
```

`importance` is optional in storage. When it is absent, the scheduling engine treats it as `NORMAL` without pretending that the user explicitly selected Normal.

## Invariants
- Task belongs to exactly one User.
- Title is required after trimming.
- Due date/time is required.
- Course is optional.
- If Course exists, it must belong to the same User.
- Estimated minutes are optional and must be greater than zero when present.
- Importance is optional.
- Notes are optional.
- `ACTIVE` implies `completedAt == null`.
- `COMPLETED` implies `completedAt != null`.

## Lifecycle Rules

### Create
Required:
- title
- dueAt

Optional:
- courseId

The initial creation path does not require effort, importance, or notes.

### Complete
- Changes status to `COMPLETED`.
- Records `completedAt`.
- Removes the Task from active recommendation consideration.

### Reopen
- Changes status to `ACTIVE`.
- Clears `completedAt`.
- Returns the Task to recommendation consideration.

### Edit
Allowed fields:
- title
- dueAt
- courseId
- estimatedMinutes
- importance
- notes

Removing optional planning data must not invalidate the Task.

### Delete
Hard deletion is not part of the first MVP Task lifecycle.

---

# Derived Planning Data

These values must not be persisted in the initial schema because they can change as time passes or Task fields change.

## RemainingTime
Derived from:
- `Task.dueAt`
- current time
- user's time zone for local-date labels

Examples:
- Due today
- 1 day left
- 4 days left
- Overdue by 2 days

## Urgency
Derived from deadline proximity and Task state.

## Recommendation
Derived from active Tasks, current time, estimated effort when available, and importance when available.

Candidate output:
- recommended Task ID
- recommendation reason code
- human-readable explanation
- start guidance

## RiskAssessment
Derived only when available data supports a conclusion.

Candidate output states:
- `AT_RISK`
- `NO_CONFIRMED_RISK`
- `INSUFFICIENT_DATA`

The engine must not invent a precise risk estimate when effort or availability information is absent.

---

# Ownership Boundary (After Authentication)

During the single-user trial there is no account ownership boundary. After authentication is introduced, every Course and Task is user-owned.

Application services receive the authenticated `userId` and query/mutate resources within that ownership boundary.

A request must never authorize access merely because a resource ID exists.

For user-owned resources:

```text
authenticated userId + resourceId
                │
                v
       ownership-aware lookup
```

Cross-user access is rejected at the backend regardless of frontend behavior.

---

# Time Model

## Persistence
Persist timestamps as absolute instants.

Examples:
- `dueAt`
- `completedAt`
- `createdAt`
- `updatedAt`

## Local Interpretation
During the single-user trial, use the browser's local zone for display-only local-calendar concepts where needed. Exact remaining-time and scheduling urgency continue to use absolute instants.

After authentication, use `User.timeZone` when a backend business rule depends on the user's local calendar day, such as "due today".

Do not store preformatted remaining-time labels.

## Testability
Time-dependent domain/application logic must receive an injectable `Clock` or explicit `Instant now` rather than calling the system clock directly throughout the codebase.

---

# Aggregate / Boundary Guidance

Keep the MVP model simple:
- User authentication state is owned by `identity`.
- Course lifecycle is owned by `course`.
- Task lifecycle is owned by `task`.
- Scheduling consumes immutable Task planning data and produces derived recommendations.

Do not create persistence entities for Recommendation, Urgency, or Risk in the MVP.

---

# Domain Acceptance Checklist

- [x] Task exists without Course.
- [x] Task exists without estimated effort.
- [x] Task exists without explicit importance.
- [x] Completed Task can be reopened.
- [x] Course removal does not delete Tasks.
- [x] Planning outputs can be regenerated from persisted state plus current time.
- [x] Cross-user Course assignment will be invalid once authentication/ownership is enabled.
- [x] Time-dependent behavior is testable with an injected clock.
