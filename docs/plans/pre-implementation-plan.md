# Pre-Implementation Plan

## Purpose
Prepare JodNgan for implementation without writing application code prematurely. This plan turns the current product requirements and UX direction into concrete design artifacts that the backend, frontend, tests, and Codex can execute against consistently.

Execution note: the current private trial intentionally validates the single-user core planning flow before authentication. `docs/plans/vertical-slices.md` is the source of truth for implementation order when this earlier plan conflicts with the revised slice sequence.

Core product principle:

> Capture work almost as quickly as Notes, then help the student decide what to work on next.

## Scope Guardrails
This plan covers only the core MVP before implementation.

In scope:
- Low-fidelity UX validation
- Final screen inventory
- Domain model
- Domain rules and derived data
- Database schema
- REST API contract
- Backend module boundaries
- Frontend route/component boundaries
- Testing strategy
- Project bootstrap readiness

Out of scope for now:
- Google Classroom integration
- Microsoft Teams integration
- AI-controlled scheduling
- Push notifications
- Redis
- Queues/event-driven architecture
- Microservices
- Kubernetes
- Real-time collaboration
- Advanced analytics

---

# Step 1 - Validate the Low-Fidelity UX

## Objective
Confirm that the primary user journey is simpler than maintaining the same workload manually in Notes and that the interface exposes only information needed for the next action.

## Inputs
- `docs/product/requirements.md`
- `docs/product/quick-capture-principle.md`
- `docs/ux/primary-flow.md`
- `docs/ux/wireframe-spec.md`

## Tasks
1. Create mobile-first low-fidelity wireframes for:
   - Today
   - Quick Add
   - Tasks
   - Task Detail
2. Design these required states:
   - no tasks
   - normal active tasks
   - due today
   - overdue
   - at risk
   - recommendation with incomplete metadata
   - completed
   - Quick Add validation error
3. Verify that Quick Add initially exposes only:
   - task name
   - due date/time
   - optional course
4. Ensure estimated effort and importance are progressive fields, not capture blockers.
5. Check that Today answers without extra navigation:
   - What should I work on next?
   - Why?
   - What is due soon?
   - How much time remains?
   - Is anything at risk?
6. Verify mobile interaction before desktop adaptation.

## Expected Outputs
- Figma low-fidelity wireframes or equivalent visual artifact
- Updated UX notes only if the wireframe reveals a requirement conflict
- Final confirmation of MVP navigation

## Acceptance Criteria
- A normal task can plausibly be entered in about 5-10 seconds by a familiar user.
- Quick Add has no more than three primary fields before save.
- Recommendation and remaining time are visible without opening Task Detail.
- Optional planning fields do not dominate the initial workflow.
- Every required UX state has an explicit representation.

## Decision Gate
Decide whether `Calendar` belongs in MVP navigation.

Default recommendation: keep Calendar out of the first implementation unless the wireframe shows that users cannot understand upcoming deadlines well enough from Today and Tasks.

## Risks
- Adding too much information to Today turns it into a dashboard instead of an action screen.
- Making Task Detail too powerful can leak complexity back into Quick Add.

---

# Step 2 - Freeze the MVP Screen Inventory

## Objective
Create a stable list of screens and actions so backend and frontend design are derived from actual user behavior rather than hypothetical features.

## Inputs
- Approved low-fidelity wireframes
- Product requirements and user stories

## Tasks
Create `docs/ux/screen-inventory.md` containing, for every screen:
- purpose
- entry points
- visible data
- user actions
- validation states
- loading state
- empty state
- error state
- navigation destinations

Expected initial inventory:
- Authentication screen(s)
- Today
- Quick Add
- Tasks
- Task Detail
- Course management surface, only if required by the approved flow
- Calendar only if Step 1 explicitly approves it

## Expected Outputs
- `docs/ux/screen-inventory.md`

## Acceptance Criteria
- Every MVP user story maps to at least one screen/action.
- Every screen exists because of a requirement, not because it is common in similar apps.
- No optional integration feature creates a screen in the MVP.

## Risks
- Creating a separate page for every concept increases navigation cost unnecessarily.

---

# Step 3 - Define the Domain Model

## Objective
Define the smallest domain model that supports the approved UX and business rules.

## Inputs
- Approved screen inventory
- `docs/product/requirements.md`

## Candidate Core Entities

### User
Likely responsibilities:
- identity
- ownership boundary for tasks and courses

Candidate fields:
- id
- email
- passwordHash or external-auth identifier depending on authentication decision
- createdAt
- updatedAt

### Course
Candidate fields:
- id
- userId
- name
- optional display metadata only if UX requires it
- createdAt
- updatedAt

### Task
Candidate fields:
- id
- userId
- courseId nullable
- title
- dueAt
- estimatedMinutes nullable
- importance nullable/defaulted
- notes nullable
- status
- completedAt nullable
- createdAt
- updatedAt

## Derived Data - Do Not Persist Initially
Unless a later requirement proves otherwise, these should be calculated rather than stored:
- timeRemaining
- overdue state
- urgency
- recommendation rank
- recommendation reason
- atRisk state
- start recommendation

## Tasks
1. Define entity invariants.
2. Define nullable vs required fields.
3. Define task status lifecycle.
4. Define ownership rules.
5. Define course deletion behavior.
6. Define which values are persisted versus derived.
7. Create a small domain diagram.

## Expected Outputs
- `docs/architecture/domain-model.md`
- Domain diagram embedded or linked from the document

## Acceptance Criteria
- Task can exist without Course.
- Task can exist without estimated effort or importance.
- Completed tasks can be reopened.
- Course removal never silently deletes tasks.
- Recommendation state can be regenerated from persisted task data plus current time.

## Open Decision
Choose the exact `importance` model for MVP.

Recommended default:
- LOW
- NORMAL
- HIGH

Avoid numeric user-facing priority scores in MVP.

---

# Step 4 - Define Scheduling and Risk Rules V1

## Objective
Turn the product promise into deterministic, explainable business rules before implementation.

## Inputs
- Domain model
- User stories US-02, US-03, US-04, US-05

## Design Principles
- Same input state produces the same recommendation.
- The engine must work when estimated effort is missing.
- Reasons shown to users must be understandable.
- Avoid false precision.
- Start simple enough to test exhaustively.

## Proposed V1 Behavior

### When effort is unknown
Base ordering primarily on deadline state:
1. overdue
2. due today
3. due soon
4. later

Importance may break ties if present.

### When effort is known
Use effort pressure in addition to deadline urgency.

Candidate concept:

`effort pressure = estimated effort / time remaining`

This is a design concept, not yet the final formula.

### At-risk status
Only mark a task at risk when enough information exists to justify the result.

If effort is unknown, show limited-confidence guidance rather than an at-risk claim based on invented assumptions.

## Tasks
1. Define deadline buckets precisely.
2. Define overdue behavior.
3. Define how importance affects ordering.
4. Define how estimated effort affects ordering.
5. Define tie-breaking rules.
6. Define recommendation reason codes/messages.
7. Define at-risk prerequisites.
8. Define edge cases around deadline equality and current time.
9. Write examples before code.

## Expected Outputs
- `docs/architecture/scheduling-rules.md`
- Table of example inputs and expected recommendation order

## Acceptance Criteria
- Rules work with missing estimated effort.
- Rules are deterministic.
- Rules produce a human-readable reason.
- Completed tasks are always excluded.
- Overdue tasks are handled explicitly.
- The rules can be unit-tested without database or HTTP dependencies.

## Major Open Decision
How should JodNgan estimate available working time?

Recommended MVP approach:
Do not attempt a full personal-calendar availability model yet. For V1, rank work using deadline pressure and estimated effort, and reserve exact hour-by-hour scheduling for a later version.

Reason: requiring users to maintain detailed availability would conflict with the Notes-speed product principle.

---

# Step 5 - Design the Database Schema

## Objective
Translate the approved domain model into a minimal PostgreSQL schema that preserves ownership and supports future evolution.

## Inputs
- `docs/architecture/domain-model.md`
- Scheduling rules
- Authentication decision

## Tasks
1. Define tables and primary keys.
2. Define foreign keys and ownership constraints.
3. Define nullability.
4. Define timestamps.
5. Define useful indexes.
6. Define course deletion behavior.
7. Define task status representation.
8. Define migration naming conventions.
9. Avoid storing derived scheduling outputs unless proven necessary.

## Candidate Tables
- users
- courses
- tasks

## Candidate Indexes
Validate with actual query patterns before finalizing, but likely candidates include:
- tasks by user and status
- tasks by user and dueAt
- courses by user

## Expected Outputs
- `docs/architecture/database.md`
- ER diagram
- Initial migration plan, but no migration code yet

## Acceptance Criteria
- Every persisted field maps to a domain requirement.
- User ownership can be enforced reliably.
- Task queries required by Today and Tasks are straightforward.
- Optional Course and effort data are represented correctly.
- Schema does not persist values that can safely be derived.

## Risks
- Premature indexing before query patterns are known.
- Encoding business rules in database fields that should remain domain logic.

---

# Step 6 - Define the REST API Contract

## Objective
Define stable frontend/backend boundaries before implementation.

## Inputs
- Screen inventory
- Domain model
- Database design
- Scheduling rules

## Candidate API Capabilities

### Authentication
Exact endpoints depend on the authentication decision.

### Tasks
Expected capabilities:
- create task
- list active/completed tasks
- get task detail
- edit task
- complete task
- reopen task
- delete task only if confirmed as an MVP requirement

### Courses
Expected capabilities:
- create course
- list courses
- edit course
- remove course without deleting tasks

### Today / Recommendation
Prefer an API designed around the UI need rather than forcing the frontend to reproduce scheduling logic.

Possible capability:
- fetch Today summary containing primary recommendation, reason, upcoming tasks, and risk indicators

## Tasks
For every endpoint define:
- method
- path
- authentication requirement
- request body
- response body
- validation errors
- not-found behavior
- authorization behavior
- example payload

## Expected Outputs
- `docs/architecture/api-contract.md`

## Acceptance Criteria
- Frontend never needs to implement scheduling business rules.
- Quick Add requires a minimal request payload.
- API supports optional effort and importance.
- User ownership is enforced server-side.
- Error shapes are consistent.

## Open Decision
Choose API versioning style.

Recommended MVP default:
Use `/api/...` without premature `/v1` unless public backwards-compatible API support is an actual requirement.

---

# Step 7 - Freeze Backend Module Boundaries

## Objective
Define modular-monolith boundaries before package structure is created.

## Inputs
- Domain model
- API contract
- ADR 0001

## Proposed Modules

### identity
Owns:
- users
- authentication/authorization concerns

### course
Owns:
- Course domain and persistence

### task
Owns:
- Task lifecycle
- task persistence
- validation of task-specific rules

### scheduling
Owns:
- recommendation engine
- urgency/risk evaluation
- recommendation explanation

## Boundary Rule
Scheduling may consume task domain data through explicit application/domain interfaces. It should not reach directly into another module's persistence internals.

## Tasks
1. Define module responsibilities.
2. Define allowed dependencies.
3. Define DTO boundary versus domain objects.
4. Define application services needed by API controllers.
5. Record any new architectural decision as ADR if the choice is significant.

## Expected Outputs
- Update `docs/architecture/overview.md` or create `docs/architecture/module-boundaries.md`

## Acceptance Criteria
- Controllers contain no recommendation rules.
- Scheduling logic is independently testable.
- Module ownership of entities is unambiguous.
- Circular dependencies are avoided by design.

---

# Step 8 - Define Frontend Routes and Component Boundaries

## Objective
Create a frontend structure that mirrors the approved UX without overbuilding a design system.

## Inputs
- Wireframes
- Screen inventory
- API contract

## Candidate Routes
Subject to final UX approval:
- `/login`
- `/today` or `/`
- `/tasks`
- `/tasks/:id`

Quick Add should preferably be an interaction from Today/Tasks rather than requiring a dedicated route unless the mobile UX benefits from a full-screen form.

## Candidate Reusable Components
Only create abstractions justified by repeated UI:
- QuickAddTask
- TaskListItem / TaskCard
- RemainingTimeLabel
- RecommendationCard
- RiskBadge
- CourseSelector

## Tasks
1. Map routes to screens.
2. Define server state vs local UI state.
3. Define shared components only where repetition exists.
4. Define loading/error/empty states.
5. Define responsive behavior.
6. Decide whether Quick Add is inline, modal, drawer, or full-screen on mobile based on wireframe testing.

## Expected Outputs
- `docs/architecture/frontend-structure.md`

## Acceptance Criteria
- Route count stays minimal.
- Business rules are not duplicated in React.
- Mobile primary flow remains fast.
- Reusable components are created because of actual reuse, not speculative abstraction.

---

# Step 9 - Define the Testing Strategy

## Objective
Specify what must be verified automatically before any feature is considered complete.

## Backend Test Layers

### Domain unit tests
Highest priority for:
- scheduling rules
- deadline calculations
- task lifecycle
- risk evaluation

These should not require Spring or a database where practical.

### Application/service tests
Verify:
- orchestration
- authorization decisions
- module interactions

### Integration tests
Use for:
- repositories
- migrations
- API behavior
- PostgreSQL-specific behavior

Testcontainers is a candidate for database integration tests once implementation begins.

## Frontend Tests
Prioritize:
- Quick Add validation and successful flow
- Today recommendation rendering
- task completion behavior
- important error/empty states

Avoid low-value tests that only assert static markup.

## End-to-End
Keep E2E small and focused on the critical journey:
1. sign in
2. add task
3. see remaining time/recommendation
4. enrich effort
5. complete task

## Expected Outputs
- `docs/architecture/testing-strategy.md`

## Acceptance Criteria
- Scheduling rules have deterministic unit coverage.
- Ownership/security cases are tested server-side.
- Database integration is tested against PostgreSQL behavior, not only mocks.
- Critical user journey has at least one end-to-end verification before MVP completion.

---

# Step 10 - Decide Authentication Approach

## Objective
Resolve authentication before project bootstrap because it affects User schema, API security, frontend routing, and tests.

## Options

### Option A - Application-owned email/password
Pros:
- demonstrates Spring Security and password hashing
- works independently of third-party providers
- straightforward for a portfolio demo

Cons:
- requires registration/login/reset considerations
- increases security responsibility

### Option B - External OAuth login
Pros:
- less password handling
- potentially convenient for users

Cons:
- OAuth setup adds external complexity early
- may distract from the core product

## Recommendation
For the MVP portfolio implementation, prefer a simple application-owned email/password authentication using established Spring Security mechanisms and strong password hashing, unless UX design reveals a strong reason to choose external OAuth.

Do not implement custom cryptography or custom token algorithms.

## Expected Output
- ADR documenting the selected authentication approach

## Acceptance Criteria
- Authentication choice is explicit before schema/API bootstrap.
- Authorization rules for user-owned resources are documented.

---

# Step 11 - Project Bootstrap Readiness Review

## Objective
Verify that implementation can begin without unresolved product or architecture ambiguity that would cause immediate rework.

## Required Artifacts Before Bootstrap
- [x] Low-fidelity wireframes approved
- [x] Screen inventory frozen for MVP
- [x] Domain model documented
- [x] Scheduling rules V1 documented with examples
- [x] Database schema documented
- [x] API contract documented
- [x] Backend module boundaries documented
- [x] Frontend route/component structure documented
- [x] Testing strategy documented
- [x] Authentication ADR accepted
- [x] No unresolved requirement contradiction blocks implementation

## Bootstrap Decision
Only after the checklist passes:
1. Bootstrap Java 21 + Spring Boot backend.
2. Bootstrap React + TypeScript frontend.
3. Add PostgreSQL local development setup.
4. Add migration tooling.
5. Add test foundations.
6. Implement the first vertical slice.

## Recommended First Vertical Slice
Implement one narrow end-to-end capability before building every layer separately:

> A single trial user opens JodNgan, creates a minimal task without signing in, and sees it returned with calculated remaining-time information.

Why first:
- directly validates the Notes-speed capture model
- touches frontend, API, domain, persistence, validation, and derived time behavior
- avoids spending the first product slice on account plumbing
- does not require the full scheduling engine yet

After that, add recommendation behavior as the next vertical slice.

---

# Open Decisions Summary

These decisions must be resolved before or during the indicated step:

1. **Calendar in MVP navigation** - resolved: deferred; primary navigation is Today + Tasks.
2. **Importance representation** - resolved: LOW / NORMAL / HIGH; nullable when not explicitly selected.
3. **Scheduling V1 rules** - resolved in `docs/architecture/scheduling-rules.md`.
4. **Available-time model** - resolved: no detailed availability calendar in MVP; use conservative clock-time pressure only.
5. **Authentication** - design resolved but implementation deferred until the core single-user flow is complete; see ADR 0002 and `vertical-slices.md`.
6. **Task deletion** - resolved: no hard delete in the first MVP; use edit, complete, and reopen.
7. **Course management surface** - resolved: lightweight secondary modal/sheet or embedded interaction; no primary navigation route.

---

# Requirement Consistency Review

## Consistent Decisions
The current documents agree on these points:
- Quick Add requires task name and due date/time; course is optional.
- Estimated effort and importance are progressive, not required for creation.
- The recommendation core is deterministic and does not depend on AI.
- Today is action-oriented rather than analytics-oriented.
- Task recommendation must work with incomplete optional metadata.
- Modular monolith is the initial architecture.
- Integrations such as Classroom and Teams are later features.

## Gaps Requiring Design Decisions
No blocking product or architecture contradiction remains before project bootstrap.

Implementation details may still evolve through normal coding feedback, but the core MVP behavior, ownership model, scheduling rules, persistence model, API boundary, authentication approach, and frontend flow are defined.

---

# Execution Order

Follow this order:

1. Low-fi wireframes
2. Screen inventory
3. Domain model
4. Scheduling/risk rules V1
5. Database schema
6. API contract
7. Backend module boundaries
8. Frontend route/component boundaries
9. Testing strategy
10. Authentication ADR
11. Bootstrap readiness review
12. Spring Boot + React bootstrap
13. First vertical slice

Do not start application implementation before Steps 1-10 are sufficiently resolved to pass the readiness review.
