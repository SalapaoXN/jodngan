# Project Bootstrap Specification

## Status
Slice 0 foundation is complete. This document records the original bootstrap decisions; the current implementation order, migration sequence, and feature status in `vertical-slices.md`, `PLANS.md`, and the current Flyway files supersede the early sequence examples below.

## Verified Baseline
Verified or locked on 2026-10-28:

### Backend
- Java 21
- Spring Boot 4.1.2
- Maven
- PostgreSQL
- Flyway

### Frontend
- Node.js 24 LTS
- React 19.3
- TypeScript
- Vite 8.3 stable line
- React Router
- TanStack Query
- React Hook Form

Exact patch versions for frontend supporting libraries should be resolved by the package manager at bootstrap time and committed through the lockfile.

---

# Backend Bootstrap

## Build Tool
Use Maven.

Reason:
- straightforward Spring Boot ecosystem support
- readable portfolio configuration
- low build-script complexity
- no project-specific requirement currently benefits from Gradle

## Coordinates

```text
groupId: com.jodngan
artifactId: jodngan-backend
name: JodNgan Backend
package: com.jodngan
java: 21
```

## Initial Spring Dependencies

Required:
- Spring Web
- Spring Validation
- Spring Security
- Spring Data JPA
- PostgreSQL Driver
- Flyway Migration
- Spring Boot Test
- Spring Security Test

Testing additions:
- Testcontainers PostgreSQL
- ArchUnit

Do not add initially:
- Redis
- Kafka
- RabbitMQ
- WebSocket
- Spring AI
- Spring Cloud

---

# Backend Initial Structure

```text
backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/jodngan/
│   │   │   ├── JodNganApplication.java
│   │   │   ├── identity/
│   │   │   ├── course/
│   │   │   ├── task/
│   │   │   ├── scheduling/
│   │   │   └── shared/infrastructure/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   └── test/
│       └── java/com/jodngan/
└── README.md
```

Do not create placeholder classes in every package merely to make directories exist.

Create code only as a vertical slice needs it.

---

# Backend Configuration

## application.yml
Configuration should use environment variables for environment-specific values.

Conceptual configuration:

```text
DB URL
DB username
DB password
session/cookie production settings
logging level
```

No real credential belongs in Git.

## Development Database
Use PostgreSQL locally.

Recommended later developer setup:
- Docker Compose with PostgreSQL only
- backend may run from IDE/Maven during development

Do not containerize every component before the first vertical slice works.

---

# Flyway

Initial migrations follow:

```text
V1__create_users.sql
V2__create_courses.sql
V3__create_tasks.sql
V4__add_initial_indexes.sql
```

The schema must match `docs/architecture/database.md`.

---

# Security Bootstrap

Implement according to ADR 0002:
- Spring Security
- application-owned email/password
- PasswordEncoder
- server-managed session
- HTTP-only session cookie
- CSRF enabled

Do not add JWT.

First security milestone:
- register
- login
- logout
- `/api/auth/me`
- authenticated route protection

---

# Frontend Bootstrap

## Tooling
Use Vite + React + TypeScript.

## Initial Structure

```text
frontend/
├── package.json
├── package-lock.json
├── vite.config.ts
├── tsconfig*.json
├── src/
│   ├── app/
│   ├── features/
│   │   ├── auth/
│   │   ├── courses/
│   │   ├── tasks/
│   │   └── today/
│   ├── components/ui/
│   ├── lib/
│   └── main.tsx
└── README.md
```

## Initial Dependencies
Runtime:
- react
- react-dom
- react-router
- @tanstack/react-query
- react-hook-form

Validation library:
- use one schema validator only if it simplifies shared form rules

Development/testing:
- TypeScript
- Vite
- ESLint
- Vitest
- Testing Library
- MSW when network-level component tests begin

Playwright can be added once the first end-to-end flow exists.

---

# Styling

Do not block foundation work on final visual design.

Initial implementation may use:
- Tailwind CSS
- a small accessible primitive/component library where useful

Rules:
- preserve Figma information hierarchy
- mobile-first
- no large custom design system before repeated patterns exist

---

# Local Frontend/Backend Integration

Development should keep API requests under:

```text
/api
```

Preferred local approach:
- Vite dev server proxies `/api` to Spring Boot
- browser continues to call relative `/api/...` paths

This reduces environment-specific frontend API URLs and keeps session-cookie behavior simpler.

---

# Quality Gates From First Commit

Backend:
- compile
- unit tests

Frontend:
- typecheck
- lint
- build

Add integration tests as persistence/security behavior appears.

Do not wait until the end of the project to introduce testing.

---

# First Implementation Sequence

Do not build all entities/controllers first.

Use vertical slices:

```text
Slice 0 - Foundation
Slice 1 - Authentication
Slice 2 - Quick Add + Remaining Time
Slice 3 - Tasks + Lifecycle
Slice 4 - Courses
Slice 5 - Scheduling Engine + Today
Slice 6 - Progressive Enrichment + Risk
Slice 7 - Quality / E2E
```

Detailed slice definitions should live in `vertical-slices.md`.

---

# Bootstrap Definition of Done

Bootstrap is complete when:
- backend starts successfully
- frontend starts successfully
- PostgreSQL connection is configured locally
- Flyway initializes successfully against PostgreSQL; the first schema migration is introduced with Slice 1
- frontend can reach a backend health/simple endpoint through `/api`
- backend tests execute
- frontend typecheck/tests/build execute
- no secrets are committed

Application feature work begins only after this foundation is reproducible.
