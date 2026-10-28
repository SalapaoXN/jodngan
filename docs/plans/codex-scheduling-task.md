# Codex Task: Implement Slice 2 Scheduling Engine + Today

## Status
Complete. Backend and frontend verification passed, including deterministic scheduling tests and the Today recommendation flow.

## Goal
Deliver JodNgan's core differentiator for the private single-user trial: given the current active Tasks, deterministically tell the user what to work on next and when to start.

This slice must work with deadline-only Tasks. Estimated effort, importance, notes, Course, authentication, ownership, notifications, AI, calendar integration, and detailed availability are still out of scope.

---

## Read First
- `AGENTS.md`
- `PLANS.md`
- `docs/plans/vertical-slices.md`
- `docs/architecture/scheduling-rules.md`
- `docs/architecture/domain-model.md`
- `docs/architecture/api-contract.md`
- `docs/architecture/module-boundaries.md`
- `docs/architecture/testing-strategy.md`
- `docs/product/requirements.md`

Slice 1 already provides:
- PostgreSQL-backed Task persistence
- Task create/list/get APIs
- active Task listing ordered by deadline
- injected `Clock`
- remaining time and overdue calculation
- Today/Quick Add UI shell

Preserve all Slice 1 behavior.

---

# Scope Guard

Implement only:
- deadline urgency bands
- deterministic ranking
- deterministic tie-breakers
- recommendation reason
- start guidance
- Today query/application service
- `GET /api/today`
- frontend RecommendationCard / Today recommendation state
- tests for the above

Do not implement:
- `estimatedMinutes`
- effort-pressure bonus
- `importance`
- risk based on workload
- Task edit/complete/reopen endpoints
- Course
- User/Auth/ownership
- notifications
- AI
- exact calendar/time-block scheduling

The scheduling types may be designed so later effort/importance can be added cleanly, but do not add unused persistence fields or speculative abstractions.

---

# Backend

## 1. Scheduling Module
Create a focused scheduling module under:

```text
com.jodngan.scheduling
├── application
└── domain
```

Keep ranking logic pure and persistence-independent.

Suggested domain types:

```text
SchedulableTask
UrgencyBand
RecommendationReason
StartGuidance
SchedulingEngine
RankedTask / RecommendationResult
```

Names may differ if an equally clear design already fits the codebase.

The scheduling domain must not import JPA repositories, controllers, or infrastructure classes.

## 2. Inputs
For Slice 2, ranking inputs are:
- Task id
- title
- dueAt
- status
- createdAt
- one captured `Instant now`

Only `ACTIVE` Tasks participate.

All calculations for one Today request must use the same captured `now`.

## 3. Deadline Urgency Bands
Implement exactly from `docs/architecture/scheduling-rules.md`:

```text
OVERDUE   dueAt < now          base 100
CRITICAL  remaining <= 24h     base 80
HIGH      remaining <= 72h     base 60
MEDIUM    remaining <= 7d      base 40
LOW       remaining > 7d       base 20
```

Deadline exactly equal to `now` is not overdue; treat it as CRITICAL / deadline reached.

Do not expose the numeric score in the normal API response or UI.

## 4. Ranking for Deadline-Only Slice
Because effort and importance are not implemented yet:

```text
recommendationScore = deadlineBaseScore
```

Sort deterministically by:
1. higher deadline base score
2. earlier dueAt
3. earlier createdAt
4. UUID lexical order

This is the Slice 2 specialization of the full V1 tie-break rules where importance is currently equal/unset for all Tasks.

The same Task state plus the same fixed `now` must always produce the same order.

## 5. Recommendation Reason
For Slice 2 only, reason precedence resolves to:

```text
OVERDUE
DUE_WITHIN_24_HOURS
DUE_SOON
UPCOMING
```

Suggested mapping:
- overdue -> `OVERDUE`
- future/reached deadline within 24h -> `DUE_WITHIN_24_HOURS`
- within 7d -> `DUE_SOON`
- later than 7d -> `UPCOMING`

Return reason code and human-readable message separately.

Use the accepted Thai messages where practical:

```text
OVERDUE              "เลยกำหนดส่งแล้ว"
DUE_WITHIN_24_HOURS  "ใกล้กำหนดส่งมาก"
DUE_SOON             "กำหนดส่งใกล้เข้ามา"
UPCOMING              "เป็นงานถัดไปตามกำหนดส่ง"
```

Do not add effort/importance reason codes to active behavior yet.

## 6. Start Guidance
Implement the deadline-only subset of V1:

```text
START_NOW    overdue OR deadline <= 24h
START_TODAY  deadline <= 72h
START_SOON   deadline <= 7d
CAN_WAIT     deadline > 7d
```

Higher guidance wins.

Return the enum/code from backend. Frontend should display a concise human-readable label without recomputing the rule.

## 7. Today Query Service
Add an application service that:
1. captures `now = clock.instant()` once
2. loads active Tasks through the Task module's application/repository contract
3. maps them to immutable scheduling input
4. invokes the pure SchedulingEngine
5. composes the Today result

Do not make the scheduling engine query Task repositories directly.

A narrow Task application contract/projection is preferred over importing Task infrastructure classes.

## 8. GET /api/today
Implement:

```text
GET /api/today
```

No authentication in the current private single-user trial.

Response when Tasks exist:

```json
{
  "generatedAt": "2026-10-29T01:00:00Z",
  "primaryRecommendation": {
    "task": {
      "id": "uuid",
      "title": "Database Assignment",
      "dueAt": "2026-10-31T05:59:00Z",
      "status": "ACTIVE",
      "createdAt": "2026-10-29T00:00:00Z",
      "updatedAt": "2026-10-29T00:00:00Z",
      "planning": {
        "remainingSeconds": 190800,
        "overdue": false
      }
    },
    "urgency": "HIGH",
    "reasonCode": "DUE_SOON",
    "reason": "กำหนดส่งใกล้เข้ามา",
    "startGuidance": "START_TODAY"
  },
  "upcomingTasks": []
}
```

Response when no active Tasks exist:

```json
{
  "generatedAt": "2026-10-29T01:00:00Z",
  "primaryRecommendation": null,
  "upcomingTasks": []
}
```

`upcomingTasks` should contain at most 5 Tasks after the primary recommendation, in the same deterministic ranking order.

Each upcoming item should contain enough information to render title, due time, remaining/overdue state, urgency, and start guidance without frontend ranking logic.

Avoid duplicating Task response mapping unnecessarily; reuse a clear mapper/result shape where it improves consistency.

---

# Frontend

## 1. Today Query
Add a TanStack Query for:

```text
GET /api/today
```

Suggested feature files:

```text
src/features/today/
├── api.ts
├── queries.ts
├── types.ts
└── RecommendationCard.tsx
```

Keeping TodayPage in the current location is acceptable if moving it would create needless churn.

## 2. RecommendationCard
When a primary recommendation exists, show prominently:
- Task title
- due date/time
- remaining/overdue label
- short recommendation reason
- start guidance

The card should answer at a glance:

```text
What should I work on next?
Why?
When should I start?
```

Do not show raw ranking scores.

Example presentation:

```text
ควรทำต่อ: Database Assignment
กำหนดส่งใกล้เข้ามา
เริ่มวันนี้
เหลือ 2 วัน
```

Exact wording may be polished, but preserve the meaning from backend codes.

## 3. Empty State
When `/api/today` returns no recommendation:
- keep Quick Add visible
- show a simple no-active-task state
- do not display a fake recommendation

## 4. Errors / Loading
Recommendation loading/error must not prevent Quick Add from being usable.

If Today recommendation fails but `/api/tasks` still works, the user should still be able to see/capture Tasks and retry the recommendation request.

## 5. Quick Add Integration
After successful task creation, invalidate/refetch both:
- active Task list query
- Today recommendation query

The newly created Task may change the recommendation immediately.

Do not calculate ranking in React.

## 6. Upcoming Tasks
Render the backend-provided upcoming list if useful for the existing Today layout.

Do not sort/re-rank it on the frontend.

Keep the list intentionally short.

---

# Tests

## Backend Domain Tests
Add pure deterministic tests for at least:
1. overdue outranks all future Tasks
2. <=24h outranks <=72h
3. <=72h outranks <=7d
4. <=7d outranks >7d
5. exact `now` is CRITICAL, not overdue
6. same urgency band -> earlier dueAt wins
7. same dueAt -> earlier createdAt wins
8. same dueAt + createdAt -> UUID lexical order wins
9. completed/ineligible input is excluded or rejected at the correct boundary
10. same inputs + fixed now -> same result

## Backend API/Application Tests
Cover:
1. no Tasks -> `primaryRecommendation: null`
2. one Task -> it is primary recommendation
3. multiple Tasks -> expected deterministic primary recommendation
4. response `generatedAt` equals fixed Clock instant
5. reason code/message correct
6. start guidance boundary cases
7. upcoming list excludes primary and is capped at 5
8. existing Slice 1 Task APIs still work

Use fixed `Clock` for all time-dependent tests.

## Frontend Tests
Cover at least:
- recommendation loading state
- no-recommendation empty state
- primary recommendation rendering
- reason rendering
- start-guidance rendering
- overdue recommendation rendering
- retry after Today API error
- successful Quick Add invalidates/refreshes recommendation
- frontend preserves backend recommendation order

Do not test internal score math in React.

---

# Verification
Run and fix:

Backend:

```text
mvn test
```

Frontend:

```text
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

Inspect:

```text
git diff --check
git status
```

Manual trial flow:

```text
open JodNgan
→ add two or more Tasks with different deadlines
→ recommendation appears
→ closer/overdue Task is recommended as expected
→ reason is understandable
→ start guidance is visible
→ add a new more urgent Task
→ recommendation updates immediately
→ refresh
→ same persisted Tasks produce the same recommendation for the current time
```

Do not stage, commit, or push.

---

# Definition of Done
Slice 2 is complete only when:
- Today answers what to work on next
- recommendation is deterministic
- deadline-only Tasks are sufficient
- reason and start guidance come from backend rules
- frontend does not duplicate ranking logic
- Quick Add causes Today recommendation to refresh
- no effort/importance/Course/Auth scope creep is introduced
- automated checks pass
- manual multi-Task recommendation flow works

---

# Final Report
Report only:
1. scheduling/domain files added
2. `/api/today` response approach
3. frontend recommendation behavior
4. backend/frontend verification results
5. manual flow result
6. unresolved blocker, if any
