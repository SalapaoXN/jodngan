# Testing Strategy

## Status
Accepted for MVP implementation.

## Goal
Protect the business rules that make JodNgan useful while keeping tests focused on behavior rather than implementation details.

---

# Backend Test Pyramid

Slice 6 adds dedicated authentication/ownership integration coverage on top of the existing Task persistence, lifecycle, scheduling, Course, and frontend behavior suites.

## 1. Domain Unit Tests
Highest priority.

Use plain JUnit tests without Spring where practical.

Cover:
- Task complete/reopen lifecycle
- deadline urgency bands
- effort pressure bands
- importance adjustment
- deterministic recommendation ordering
- tie-breaking
- recommendation reasons
- start guidance
- risk states
- due-today behavior with time zones
- incomplete effort metadata

Time-dependent tests use fixed `Clock`/`Instant` values.

### Required scheduling boundary cases
At minimum test:
- overdue
- deadline exactly now
- 24h boundary
- 72h boundary
- 7d boundary
- unknown effort
- effort pressure at 0.10 / 0.25 / 0.50 boundaries
- LOW / NORMAL / HIGH / unset importance
- identical scores and deterministic tie-breaks
- completed task exclusion
- estimated effort equal to remaining clock minutes -> AT_RISK

---

## 2. Application Service Tests
Test use-case orchestration without requiring full HTTP when possible.

Cover:
- Quick Add creates minimal Task
- optional Course ownership validation
- cross-user Task access rejected
- cross-user Course assignment rejected
- Task update recalculates planning result
- complete removes Task from active recommendations
- reopen returns Task to recommendation consideration
- Course removal leaves Tasks intact/unassigned
- Today query requests active Tasks and returns one primary recommendation

Mock only module boundaries that are genuinely external to the unit under test.

Avoid mocking simple value objects/domain behavior.

---

## 3. Repository / Database Integration Tests
Use PostgreSQL behavior, preferably through Testcontainers.

Cover:
- Flyway migrations apply from empty database
- User email uniqueness
- Task status/completed_at check constraint
- positive estimated_minutes constraint
- valid enum-like values
- Course delete sets Task course_id to null
- user ownership query methods
- active Task query ordering
- completed Task query ordering

Do not use an in-memory database as the only database verification if production uses PostgreSQL.

---

## 4. API Integration Tests
Use Spring Boot test support / MockMvc or equivalent.

Cover current core behavior first:
- Quick Add minimal payload
- validation error shape
- Task update clearing optional fields
- complete/reopen endpoints
- Today response shape
- Course CRUD
- unexpected internal details are not leaked in errors

Slice 6 additionally covers register/login/logout/current-user, unauthenticated access, CSRF, session persistence, and cross-user not-found behavior.

Focus on contract behavior from `api-contract.md`.

---

# Frontend Tests

## Component / Interaction Tests
Use Vitest + Testing Library or equivalent.

Prioritize user-visible behavior:
- Quick Add required-field validation
- Quick Add successful submission
- optional Course does not block save
- Today empty state
- Today recommendation rendering
- risk/limited-data message rendering
- Active/Completed Task filtering
- complete/reopen interaction
- Task Detail optional-field editing
- API validation errors displayed near relevant fields

Avoid tests that only assert implementation structure or static class names.

---

# API Mocking in Frontend Tests

Use a network-level mock tool such as MSW where it improves realism.

Prefer mocking HTTP behavior over mocking every query hook implementation.

This allows components to exercise:
- loading
- success
- validation errors
- server errors

with the same API shapes expected in production.

---

# End-to-End Tests

Use Playwright or equivalent for a deliberately small critical suite.

## Critical Journey

```text
Register / Sign In
      ↓
Quick Add Task
      ↓
Task appears with remaining time
      ↓
Today shows recommendation
      ↓
Open Task Detail
      ↓
Add estimated effort
      ↓
Recommendation/planning updates
      ↓
Complete Task
      ↓
Task disappears from active recommendation
      ↓
Reopen Task
```

One stable happy-path E2E test is more valuable initially than dozens of brittle UI-path tests.

## Additional high-value E2E cases
Add only after core flow is stable:
- validation on Quick Add
- cross-session persistence after login
- mobile viewport smoke test

---

# Architecture Tests

Use ArchUnit or an equivalent lightweight approach to enforce a few valuable boundaries:

- domain packages must not depend on controller/API packages
- domain packages must not depend on infrastructure packages
- scheduling must not depend on task persistence implementation
- controllers must not access repositories directly

Keep architecture tests narrow and readable.

---

# Security Tests

These checks are required in Slice 6 and are covered by the dedicated authentication/ownership integration suite:

Required server-side checks:
- password is encoded, never stored as plaintext
- invalid login does not reveal account existence
- unauthenticated API requests are rejected
- authenticated User cannot read another User's Task
- authenticated User cannot mutate another User's Task
- authenticated User cannot use another User's Course ID
- CSRF protects browser-session mutation endpoints
- API errors do not expose password hashes or stack traces

---

# Test Data

Use builders/fixtures that make business intent visible.

Example concept:

```text
activeTask()
  .dueInHours(24)
  .estimatedMinutes(180)
  .importance(HIGH)
```

Avoid giant shared fixtures where changing one default silently changes unrelated tests.

---

# Determinism

Scheduling tests must control:
- `now`
- time zone
- Task IDs when tie-breaking is relevant
- creation timestamps

Never depend on the real current clock in deterministic domain tests.

---

# CI Test Groups

Initial GitHub Actions pipeline should eventually run:

```text
Backend
- compile
- unit tests
- integration tests

Frontend
- typecheck
- lint
- unit/component tests
- production build
```

E2E may run after the application is sufficiently bootstrapped and stable.

---

# Definition of Done for a Feature

A feature is not complete until:
- relevant acceptance criteria pass
- business rules have automated tests
- authorization/validation paths are covered when applicable
- backend tests pass
- frontend tests/typecheck pass when frontend changed
- no unrelated changes are included

---

# What Not to Optimize For

Do not target a vanity coverage percentage as the primary goal.

Prefer coverage of:
- decision branches
- boundary values
- security rules
- failure behavior
- user-critical flow

over testing getters, trivial DTOs, or framework internals.

---

# Acceptance Checklist

- [x] Scheduling is testable without Spring/database.
- [x] PostgreSQL-specific behavior has integration coverage.
- [x] Ownership/security has backend coverage.
- [x] Frontend tests focus on visible behavior.
- [x] Critical user journey has an E2E plan.
- [x] Module boundaries have enforceable tests.
