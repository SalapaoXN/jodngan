# JodNgan Development Plan

## Phase 0 - Product Definition
- Define the pain point and target user.
- Record assumptions and current alternatives.
- Define MVP scope and explicit non-goals.
- Define success criteria for the first usable version.

## Phase 1 - UX and System Design
Detailed execution plan: `docs/plans/pre-implementation-plan.md`

- Create the primary user flow.
- Produce low-fidelity wireframes before implementation.
- Define domain model and module boundaries.
- Design the initial database schema and API contract.
- Record major architectural decisions as ADRs.

## Phase 2 - Project Foundation
Status: complete. Slice 0 foundation is verified.

Bootstrap references:
- `docs/plans/bootstrap-spec.md`
- `docs/plans/vertical-slices.md`
- `docs/plans/codex-bootstrap-task.md`

- Bootstrap Spring Boot backend and React + TypeScript frontend.
- Configure PostgreSQL and database migrations.
- Add validation, error handling, logging, and test foundations.
- Add local development configuration without committing secrets.

## Phase 3 - Core Product
Status: Slice 1 Core Capture through Slice 6 Authentication + Ownership are complete and verified end to end.

Implementation task references:
- `docs/plans/codex-core-task.md`
- `docs/plans/codex-scheduling-task.md`
- `docs/plans/codex-lifecycle-task.md`
- `docs/plans/codex-enrichment-risk-task.md`
- `docs/plans/codex-courses-task.md`
- `docs/plans/codex-auth-ownership-task.md`

Current target: begin Slice 7 Quality + End-to-End.

Current mode: authenticated multi-user MVP. Email/password sessions, CSRF protection, and Task/Course ownership are active and verified end to end.

- [x] Quick task capture and deadline management.
- [x] Remaining-time / overdue feedback.
- [x] Recommendation for what to work on next.
- [x] Task lifecycle and redesigned task-management UI verified end to end.
- [x] Progressive planning fields and conservative risk verified end to end.
- [x] Optional Course organization verified end to end.
- [x] User account, session authentication, CSRF handling, and Task/Course ownership verified end to end.

## Phase 4 - Scheduling Engine
- Calculate recommended work order from deadline, effort, priority, and available time.
- Handle overdue tasks and schedule changes.
- Add deterministic tests for business rules and edge cases.

## Phase 5 - Frontend Integration
- Implement the agreed Figma flow.
- Integrate frontend with backend APIs.
- Add loading, empty, validation, and error states.
- Verify the main user journey end to end.

## Phase 6 - Quality
- Expand unit and integration tests.
- Review authentication, authorization, input validation, and error handling.
- Review accessibility and responsive behavior.
- Refactor only where repeated complexity justifies it.

## Phase 7 - Delivery
- Add Docker-based local setup.
- Add GitHub Actions for build and tests.
- Deploy a demo environment.
- Finish README, screenshots, architecture diagram, and demo instructions.

## Later / Optional
- Notifications.
- Redis only if a measured use case justifies it.
- AI assistant only after the deterministic core works without AI.
- Multi-agent workflows only for clearly independent review or investigation tasks.
