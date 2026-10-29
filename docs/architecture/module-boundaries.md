# Backend Module Boundaries

## Status
Target modular-monolith boundaries accepted. Current single-user trial implements `task` first; `identity`, `course`, and `scheduling` are added only when their slices begin.

## Architecture
JodNgan backend is one Spring Boot deployable application organized as a modular monolith.

Target modules:

```text
identity
course
task
scheduling
```

Current Slice 1 implements only `task` plus existing shared technical infrastructure. Do not create empty placeholder modules.

Infrastructure shared across the application should stay small and technical rather than becoming a catch-all business module.

---

# Dependency Direction

```text
                    ┌──────────────┐
                    │   identity   │
                    └──────────────┘
                           │ authenticated user context
                           v
┌──────────────┐      ┌──────────────┐
│    course    │ <─── │     task     │
└──────────────┘      └──────┬───────┘
                             │ planning view
                             v
                      ┌──────────────┐
                      │  scheduling  │
                      └──────────────┘
```

This diagram describes conceptual use, not permission for arbitrary package imports.

Key rule:
- modules expose narrow application/domain contracts
- one module must not reach directly into another module's repository implementation

---

# Suggested Package Shape

```text
com.jodngan
│
├── identity
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── course
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── task
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── scheduling
│   ├── api
│   ├── application
│   └── domain
│
└── shared
    └── infrastructure
```

`shared` is only for genuinely cross-cutting technical concerns such as error handling, clock configuration, request IDs, or security adapters.

Do not put business entities in `shared`.

---

# Layer Meaning Inside a Module

## api
HTTP boundary only:
- controllers
- request DTOs
- response DTOs
- mapping between API and application commands/results

Controllers must not contain recommendation formulas, ownership rules, or persistence logic.

## application
Coordinates use cases:
- transactional boundaries
- loading owned resources
- invoking domain behavior
- calling explicit module contracts
- returning application results

Examples:
- `CreateTaskService`
- `UpdateTaskService`
- `CompleteTaskService`
- `TodayQueryService`

## domain
Business behavior independent of Spring MVC and persistence details.

Examples:
- Task lifecycle
- Importance / TaskStatus
- SchedulingEngine
- RiskEvaluator
- RecommendationReason

## infrastructure
Technical adapters:
- JPA entities/repositories where needed
- repository implementations
- Spring Security integration
- database mappings

Domain code should not depend on infrastructure packages.

---

# identity Module (Deferred Until Authentication Slice)

## Owns
- User account
- normalized email identity
- password hash
- user time zone
- authentication
- authenticated principal mapping

## Main Use Cases
- Register
- Login
- Logout
- Get current user

## Exposes
A minimal authenticated-user identity to application code, conceptually:

```text
AuthenticatedUser
- userId
- timeZone
```

Other modules should not need password hashes or authentication internals.

## Does Not Own
- Tasks
- Courses
- Scheduling

---

# course Module (Deferred Until Course Slice)

## Owns
- Course entity
- Course repository
- Course create/rename/remove lifecycle

## Main Use Cases
- Create Course
- List Courses
- Rename Course
- Remove Course

## Contract Needed by task
Task needs to validate that an optional Course belongs to the authenticated User.

Expose a narrow lookup capability, for example conceptually:

```text
CourseLookup
- findOwnedCourse(userId, courseId)
- existsOwnedCourse(userId, courseId)
```

Do not expose the Course repository implementation directly.

## Course Removal Coordination
Removing a Course must leave Tasks intact.

Database `ON DELETE SET NULL` is the structural fallback.

Application behavior should remain consistent with that rule.

---

# task Module

## Owns
- Task entity/lifecycle
- Task persistence
- task CRUD operations required by MVP
- ownership-aware Task lookup after authentication is introduced

## Main Use Cases
- Quick Add Task
- List Tasks
- Get Task Detail
- Update Task
- Complete Task
- Reopen Task

## Depends On
Course public application contract only when validating/changing a Course assignment.

## Exposes to scheduling
Scheduling should consume immutable planning projections rather than Task persistence objects.

Current Slice 4 contract:

```text
TaskPlanningData
- id
- title
- dueAt
- estimatedMinutes?
- importance?
- createdAt
- updatedAt
```

The Task application boundary exposes only ACTIVE planning data. Notes stay inside Task because scheduling does not need them. Course is added only when the Course slice begins.

Task remains the source of truth; scheduling does not own or mutate it.

---

# scheduling Module

The module exposes the Today HTTP boundary through `scheduling.api`, orchestration through `scheduling.application`, and persistence-independent ranking rules through `scheduling.domain`.

## Owns
- Remaining-time calculation needed for planning responses
- urgency evaluation
- recommendation ranking
- recommendation reason
- start guidance
- risk assessment
- Today orchestration/query composition

## Core Rule
The ranking algorithm is pure with respect to persistence.

Conceptually:

```text
SchedulingEngine.rank(tasks, planningContext)
```

It must not:
- query JPA repositories directly
- call HTTP APIs
- read the system clock repeatedly
- modify Tasks

## Time
Receive one `Clock`/`Instant now` for a calculation.

## Today Query
During the single-user trial, an application service may:
1. request active planning views from Task
2. use the planning context supplied for the trial
3. invoke SchedulingEngine
4. compose Today response

After authentication is added, the authenticated User time zone becomes the authoritative per-user zone.

---

# DTO Boundaries

Do not return JPA entities directly from controllers.

Preferred flow:

```text
HTTP Request DTO
      │
      v
Application Command
      │
      v
Domain / Repository
      │
      v
Application Result
      │
      v
HTTP Response DTO
```

This keeps external API changes from forcing persistence-model changes and prevents lazy-loading details from leaking into HTTP serialization.

---

# Ownership Rule (After Authentication)

The current private single-user trial has no account ownership boundary.

After authentication is implemented, every Task/Course application operation receives authenticated `userId`.

Preferred repository capability:

```text
findByIdAndUserId(...)
```

Avoid:

```text
findById(...)
then check ownership much later
```

Ownership should be part of the lookup/use-case boundary whenever practical.

---

# Transaction Boundaries

Transactions belong around application use cases that mutate state.

Examples:
- register User
- create/update/complete/reopen Task
- create/rename/remove Course

Pure scheduling calculations do not need a database transaction.

Read-only query services may use read-only transactions where useful.

---

# Cross-Cutting Technical Concerns

Allowed shared technical infrastructure:
- global exception-to-error-response mapping
- request/correlation ID
- application `Clock` bean
- security configuration
- JSON/time configuration
- logging configuration

Do not create generic `Utils`, `CommonService`, or `BaseRepository` abstractions unless concrete repetition justifies them.

---

# Boundary Enforcement

During implementation add architecture tests, preferably with ArchUnit or an equivalent simple rule set, for important constraints such as:

- domain packages do not depend on API/controller packages
- domain packages do not depend on infrastructure packages
- scheduling does not import task infrastructure/repository implementations
- controllers do not access repositories directly

Do not build an elaborate framework solely to enforce modules.

---

# Future Modules

Possible later modules:

```text
integration
notification
calendar
```

Classroom/Teams adapters should eventually feed normalized Task/application commands rather than bypassing Task rules.

They are not part of MVP implementation.

---

# Acceptance Checklist

- [x] Current single-user mode is explicit; future entity ownership rules are defined for the authentication slice.
- [x] Controllers contain no business formulas.
- [x] Scheduling is independently testable.
- [x] Scheduling does not own Task persistence.
- [x] Cross-module repository access is prohibited.
- [x] Course ownership validation has an explicit contract.
- [x] Shared package remains technical only.
