# JodNgan

JodNgan is a portfolio-oriented software engineering project for planning student workload, deadlines, and study schedules.

## Goal
Build a real, testable application that demonstrates software engineering decisions rather than a tutorial CRUD project.

## Initial Product Idea
JodNgan combines deadlines, estimated effort, priority, and available time to recommend what a student should work on next and identify schedule conflicts.

## Planned Stack
- Java 21
- Spring Boot
- PostgreSQL
- React + TypeScript
- Docker
- GitHub Actions

## Architecture
Start as a modular monolith. Add infrastructure only when a concrete requirement justifies it.

## Project Status
Phase 2 foundation bootstrap is complete. Frontend checks pass, backend tests pass with PostgreSQL Testcontainers/Flyway, and the project is ready to begin Slice 1 (Authentication).

## Repository Guide
- `AGENTS.md` - persistent repository rules for coding agents.
- `PLANS.md` - development roadmap.
- `docs/product/` - pain point, assumptions, MVP, and product decisions.
- `docs/architecture/` - architecture and ADRs.
- `docs/ux/` - user flows and design references.
- `.agents/skills/` - project-local reusable agent skills added only when justified.

## Local Development
1. Copy `.env.example` to `.env` and replace the local database password.
2. Start PostgreSQL with `docker compose up -d postgres`.
3. Export the same `DB_PASSWORD` in the shell that will run Spring Boot (PowerShell: `$env:DB_PASSWORD="..."`).
4. In `backend/`, run `mvn spring-boot:run`.
5. In `frontend/`, run `npm ci` and `npm run dev`.
6. Open the Vite URL and confirm the temporary foundation screen reports `API status: ok`.

The Vite development server proxies `/api` to Spring Boot on port 8080.
