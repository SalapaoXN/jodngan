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
Slice 1 Core Capture through Slice 6 Authentication + Ownership are complete and verified end to end. Email/password accounts use Spring Security server sessions, unsafe browser requests use CSRF protection, and Tasks/Courses are scoped to the authenticated User. The first ownership migration intentionally resets the earlier private trial data because it has no trustworthy owner mapping. Slice 7 Quality + End-to-End is the next target.

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
4. In `backend/`, run `mvn spring-boot:run` (Spring Boot defaults to port `8081`).
5. In `frontend/`, run `npm ci` and `npm run dev`.
6. Open the Vite URL (normally `http://localhost:5173`) and verify Quick Add can create a Task and that it remains after refresh.

Default local development ports:
- `8081` - JodNgan Spring Boot backend
- `5173` - Vite frontend
- `5432` - PostgreSQL

Port `8080` is intentionally left free for local tooling when needed. The Vite development server proxies `/api` to Spring Boot on port `8081`.
