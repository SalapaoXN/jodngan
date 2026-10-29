# MVP Vertical Slices

## Goal
Implement JodNgan as working end-to-end behavior instead of building every backend layer first and integrating late.

Each slice should leave the repository in a runnable, tested state.

Current trial mode is intentionally single-user: no Sign In/Register screen, no session requirement, and no ownership boundary until the core planning flow is proven useful. Do not expose this trial build directly to the public internet.

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

# Slice 1 - Quick Add + Remaining Time

**Status: complete and verified end to end.**

## Goal
Prove the Notes-speed capture value with the smallest usable Task flow.

## Backend
Implement:
- single-user Task persistence
- first Task migration
- Task minimum: title, dueAt, status, timestamps
- create Task
- list/get Task
- remaining-time calculation
- overdue display data

No User, Course, authentication, ownership, effort, importance, or notes are required yet.

## Frontend
Implement:
- app opens directly to Today
- Today empty state
- Quick Add with task name + due date/time
- save
- display created Task immediately
- remaining-time / overdue label

## Tests
- minimal Task creation
- title required
- deadline required
- past deadline allowed and shown overdue
- remaining time uses an injectable/fixed clock in tests

## Done When
A person can open JodNgan, add a Task immediately without creating an account, and see useful deadline information.

---

# Slice 2 - Scheduling Engine + Today

**Status: complete and verified.**

## Goal
Deliver the core differentiator early: tell the user what to work on next.

## Backend
Implement the deterministic scheduling behavior that can operate with deadline-only Tasks first:
- urgency bands
- deterministic ranking
- tie-breakers
- recommendation reason
- start guidance
- Today query service
- `GET /api/today`

Effort and importance remain unknown in this slice and must not be required.

## Frontend
Implement:
- RecommendationCard
- recommendation reason
- start guidance presentation
- short upcoming list
- Today normal/empty/error states

## Tests
- overdue
- deadline boundary cases
- identical score tie-breaks
- completed Task exclusion when lifecycle exists
- same Task state + fixed time produces the same recommendation

## Done When
The trial user can add multiple Tasks and JodNgan clearly answers which one should be worked on next.

---

# Slice 3 - Tasks + Lifecycle

**Status: complete and verified end to end.**

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
- single-user resource lookups; ownership-specific coverage remains deferred to Slice 6
- optional fields may be cleared when introduced

## Done When
The user can maintain the full required Task lifecycle without hard deletion.

---

# Slice 4 - Progressive Enrichment + Risk

**Status: complete and verified end to end.**

## Goal
Make recommendations smarter without increasing initial capture friction.

## Backend
Implement/edit support for:
- estimatedMinutes
- importance
- notes
- effort-pressure calculation
- risk assessment rules

Recommendation must update immediately when effort/importance changes.

## Frontend
Task Detail adds:
- estimated effort
- importance
- notes
- risk explanation
- limited-data explanation when effort is unknown

Quick Add remains title + due date/time only.

## Tests
- adding effort can change ranking
- removing effort returns to deadline-based behavior
- importance adjustment works
- conservative AT_RISK rule
- effort unknown -> INSUFFICIENT_DATA
- overdue -> AT_RISK

## Done When
The user can improve recommendation quality after capture while initial entry stays fast.

---

# Slice 5 - Courses

**Status: complete and verified end to end.**

## Goal
Add optional organization only after the planning loop already works.

## Backend
Implement:
- Course entity/persistence
- create/list/rename/remove Course
- optional Task course assignment
- Course deletion leaves Tasks unassigned

No ownership rules are required until the authentication slice.

## Frontend
Implement:
- CourseSelector
- lightweight CourseManager
- optional Course in Quick Add
- Course in Task Detail
- Course filter in Tasks

## Tests
- Task creation works without Course
- Course assignment works
- remove Course preserves Tasks
- Course filter works

## Done When
Course improves organization but never becomes a prerequisite for capture.

---

# Slice 6 - Authentication + Ownership

## Goal
Add accounts only after the core planning product is useful enough to justify multi-user persistence.

## Backend
Implement according to ADR 0002:
- User persistence
- email normalization
- password encoding
- register/login/logout/current user
- Spring Security server sessions
- CSRF handling
- Task/Course ownership
- safe migration/claim strategy for any existing single-user trial data if preservation is required

## Frontend
Implement:
- Sign In
- Register
- current-user query
- AuthGuard
- logout

## Tests
- password never stored plaintext
- duplicate email
- invalid login generic response
- unauthenticated protected request
- session survives normal requests
- CSRF protects mutations
- cross-user Task/Course access rejected

## Done When
Accounts work without changing the already-proven capture and planning experience.

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
quick add
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

Do not begin optional Classroom, Teams, AI, Calendar sync, or notification work before Slice 7 is stable. Do not pull authentication forward again unless the single-user core is already usable or a concrete multi-user requirement appears.
