# MVP Vertical Slices

## Goal
Implement JodNgan as working end-to-end behavior instead of building every backend layer first and integrating late.

Each slice should leave the repository in a runnable, tested state.

---

# Slice 0 - Foundation

## Goal
Create a reproducible backend/frontend/database development foundation.

## Backend
- Spring Boot project on Java 21 / Maven
- base package `com.jodngan`
- PostgreSQL configuration through environment variables
- Flyway enabled
- global API error envelope foundation
- injectable `Clock`
- minimal health/smoke endpoint if needed for integration verification

## Frontend
- Vite + React + TypeScript
- React Router
- TanStack Query provider
- shared API client
- Vite `/api` proxy
- basic app shell

## Local Infrastructure
- PostgreSQL Docker Compose service
- example environment/config documentation without secrets

## Verification
- backend compiles/tests
- frontend typecheck/lint/build
- frontend can reach backend through `/api`
- Flyway starts successfully

## Done When
A fresh developer can reproduce the foundation from repository instructions.

---

# Slice 1 - Authentication

## Goal
Give every later feature a real user ownership boundary.

## Backend
Implement:
- User persistence
- V1 users migration
- email normalization
- password encoding
- register
- login
- logout
- current user
- Spring Security session configuration
- CSRF handling

## Frontend
Implement:
- Sign In page
- Register page
- current-user query
- AuthGuard
- logout behavior

## Tests
- password never stored plaintext
- duplicate email
- invalid login generic response
- unauthenticated protected request
- session survives normal navigation/request
- CSRF protects mutation

## Done When
A user can register, become authenticated, refresh, and access only authenticated routes.

---

# Slice 2 - Quick Add + Remaining Time

## Goal
Prove the central Notes-speed capture value with the smallest complete Task flow.

## Backend
Implement:
- Task domain minimum: title, dueAt, status, ownership
- V3 Task migration as needed after Course dependency planning
- create Task
- get Task
- remaining-time calculation
- overdue/due-today display data

Course may remain null for this first Task slice.

## Frontend
Implement:
- Today empty state
- Quick Add with title + due date/time
- save
- display created Task immediately
- remaining-time label

## Tests
- minimal Task creation
- title required
- deadline required
- past deadline allowed and shown overdue
- timezone-aware due-today
- cross-user Task inaccessible

## Done When
An authenticated user can add a Task quickly and immediately see useful deadline information.

---

# Slice 3 - Tasks + Lifecycle

## Goal
Complete the basic Task management loop.

## Backend
Implement:
- active/completed Task list
- Task Detail
- edit Task
- complete
- reopen
- status/completedAt invariants

## Frontend
Implement:
- Tasks page
- Active / Completed tabs
- Task Detail
- complete/reopen controls
- loading/empty/error states

## Tests
- completed Task leaves active list
- reopen returns it
- status/completedAt consistency
- owned resource lookups
- optional fields may be cleared when introduced

## Done When
The user can maintain the full required Task lifecycle without hard deletion.

---

# Slice 4 - Courses

## Goal
Add optional organization without slowing Quick Add.

## Backend
Implement:
- Course entity/persistence
- V2 Course migration if migrations are created in schema order from the start
- create/list/rename/remove Course
- Course ownership lookup contract
- optional Task course assignment
- Course deletion leaves Tasks unassigned

## Frontend
Implement:
- CourseSelector
- lightweight CourseManager
- optional Course in Quick Add
- Course in Task Detail
- Course filter in Tasks

## Tests
- Task creation works without Course
- another user's Course cannot be assigned
- remove Course preserves Tasks
- Course filter is ownership scoped

## Done When
Course improves organization but never becomes a prerequisite for capture.

---

# Slice 5 - Scheduling Engine + Today

## Goal
Deliver the core differentiator: tell the user what to work on next.

## Backend
Implement pure scheduling domain behavior from `scheduling-rules.md`:
- urgency bands
- importance adjustment
- effort-pressure calculation infrastructure
- deterministic score/ranking
- tie-breakers
- recommendation reason
- start guidance
- Today query service
- `GET /api/today`

At this slice, unknown effort must already work correctly.

## Frontend
Implement:
- RecommendationCard
- recommendation reason
- start guidance presentation
- short upcoming list
- Today normal/empty/error states

## Tests
Implement all scheduling boundary tests before considering this slice complete.

## Done When
The same Task state and fixed time always produce the same explainable recommendation.

---

# Slice 6 - Progressive Enrichment + Risk

## Goal
Make planning smarter without increasing initial capture friction.

## Backend
Implement/edit support for:
- estimatedMinutes
- importance
- notes
- risk assessment rules

Recommendation must update immediately when effort/importance changes.

## Frontend
Task Detail adds:
- estimated effort
- importance
- notes
- risk explanation
- limited-data explanation when effort unknown

Quick Add remains unchanged except optional Course.

## Tests
- adding effort can change ranking
- removing effort returns to deadline-based behavior
- importance adjustment works
- conservative AT_RISK rule
- effort unknown -> INSUFFICIENT_DATA
- overdue -> AT_RISK

## Done When
The user can improve recommendation quality after capture while the initial flow stays lightweight.

---

# Slice 7 - Quality + End-to-End

## Goal
Turn the working MVP into a portfolio-quality implementation.

## Backend
- integration tests with PostgreSQL/Testcontainers
- architecture tests
- security review
- logging/error consistency

## Frontend
- responsive/mobile review against Figma
- keyboard/accessibility review
- component interaction tests
- polish loading/error/empty states

## End-to-End
Critical Playwright journey:

```text
register
→ quick add
→ see remaining time
→ see recommendation
→ add effort
→ see planning update
→ complete
→ reopen
```

## Delivery Preparation
- Docker local setup verified
- GitHub Actions
- README setup instructions
- screenshots
- architecture diagram

## Done When
The core MVP Definition of Done in `docs/product/requirements.md` is demonstrably satisfied.

---

# Slice Rules

For every slice:
1. Read relevant requirement/user story.
2. Implement the smallest coherent end-to-end behavior.
3. Add tests with the business rule, not later.
4. Run relevant backend/frontend verification.
5. Inspect diff for scope creep.
6. Update docs only when an implementation decision materially changes the agreed design.

Do not begin optional Classroom, Teams, AI, Calendar sync, or notification work before Slice 7 is stable.
