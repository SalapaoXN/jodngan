# Slice 4 - Progressive Enrichment + Risk Implementation Task

## Status
Complete. Backend/frontend automated verification and manual enrichment/risk smoke passed.

## Goal
Improve recommendation quality after capture without adding friction to Quick Add.

## Backend Scope
- Add nullable Task planning fields:
  - `estimatedMinutes`
  - `importance` (`LOW`, `NORMAL`, `HIGH`)
  - `notes`
- Preserve null as "not provided"; optional planning fields can be cleared later.
- Keep Quick Add payload limited to title + dueAt.
- Extend Task Detail update to replace title, dueAt, and optional planning fields.
- Activate the accepted V1 scheduling rules from `docs/architecture/scheduling-rules.md`:
  - deadline base score
  - effort-pressure bonus
  - importance adjustment
  - deterministic tie-breakers
  - enriched recommendation reasons
  - enriched start guidance
  - conservative risk assessment
- Do not expose the internal recommendation score in normal API/UI responses.

## Risk Rules
For ACTIVE tasks:
- overdue -> `AT_RISK / OVERDUE`
- effort unknown -> `INSUFFICIENT_DATA / EFFORT_UNKNOWN`
- known effort where estimated minutes >= clock minutes remaining -> `AT_RISK / INSUFFICIENT_CLOCK_TIME`
- otherwise -> `NO_CONFIRMED_RISK`

Completed tasks have no active risk assessment.

## Frontend Scope
- Quick Add remains task name + due date/time only.
- Task Detail adds progressive planning fields:
  - estimated effort
  - importance
  - notes
- Task Detail shows risk state and a short explanation.
- Today continues to trust backend ranking and reason data; React must not re-score tasks.
- Surface confirmed risk on Today without turning the page into an analytics dashboard.

## Explicit Non-Goals
Do not add:
- Course
- authentication/session/ownership
- detailed availability/calendar scheduling
- exact completion probability
- AI
- hard delete

## Verification
- [x] Backend: `mvn test`
- [x] Frontend: `npm run typecheck`
- [x] Frontend: `npm run lint`
- [x] Frontend: `npm test`
- [x] Frontend: `npm run build`
- [x] Manual enrichment/risk smoke

## Manual Smoke
```text
create a task through Quick Add
-> confirm effort/importance/notes were not required
-> open Task Detail
-> add effort + importance + notes
-> save
-> confirm risk explanation updates
-> return to Today and confirm recommendation/reason can update
-> clear optional planning fields
-> save and confirm task remains valid
-> confirm risk becomes limited when effort is unknown
```

## Done When
- optional planning fields persist and can be cleared
- recommendation reacts deterministically to effort/importance
- risk follows the conservative accepted V1 rules
- Quick Add remains minimal
- automated and manual verification pass
- no Slice 5/6 behavior leaks into the implementation
