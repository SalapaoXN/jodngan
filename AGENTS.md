# JodNgan

JodNgan is a student workload planning and scheduling application.

## Stack
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL
- Frontend: React + TypeScript
- Architecture: Modular Monolith

## Engineering Rules
- Keep business logic out of controllers.
- Prefer simple designs over premature abstraction.
- Database changes require migrations.
- New business logic requires tests.
- Do not introduce microservices without an ADR.
- Never commit secrets or credentials.
- Keep APIs explicit and validate external input.

## Verification
- Run relevant backend and frontend tests before declaring work complete.
- Inspect the final diff for unrelated changes.

## Context
- Product: docs/product/
- Architecture: docs/architecture/
- Roadmap: PLANS.md
- For UI/UX design, interaction refinement, or accessibility review, read `.agents/skills/ux-ui-product-design/SKILL.md` before making material interface changes.
