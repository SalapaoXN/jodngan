# Scheduling and Risk Rules V1

## Status
Accepted for the first deterministic MVP.

## Goal
Answer two questions without AI or hidden assumptions:

1. What should the user work on next?
2. Is there enough evidence to warn that a Task is at risk?

The first version deliberately ranks work rather than building an hour-by-hour personal calendar.

---

# Inputs

For each Task:
- `status`
- `dueAt`
- `estimatedMinutes?`
- `importance?`
- `createdAt`

Context:
- current `Instant`
- user's `ZoneId`

The engine must not require estimated effort.

---

# Eligibility

Only `ACTIVE` Tasks participate in recommendation ranking.

`COMPLETED` Tasks:
- are excluded from recommendations
- are excluded from active risk warnings
- may still display historical deadline information in Task Detail

---

# Remaining Time

Let:

```text
remainingMinutes = minutes between now and dueAt
```

Interpretation:
- `< 0` -> overdue
- `= 0` -> deadline reached
- `> 0` -> future deadline

User-facing labels may use the user's local time zone:
- Due today
- 1 day left
- 4 days left
- Overdue by 2 days

The label itself is not persisted.

---

# Deadline Urgency Band

The V1 engine uses elapsed clock time rather than an availability calendar.

| Band | Condition | Base score |
| --- | --- | ---: |
| OVERDUE | `dueAt < now` | 100 |
| CRITICAL | remaining `<= 24h` | 80 |
| HIGH | remaining `<= 72h` | 60 |
| MEDIUM | remaining `<= 7d` | 40 |
| LOW | remaining `> 7d` | 20 |

These numbers are internal ordering weights, not percentages and not user-facing scores.

---

# Effort Pressure

When `estimatedMinutes` is absent:

```text
effortPressureBonus = 0
```

The Task remains fully eligible for recommendation.

When effort is known and the deadline is still in the future:

```text
pressureRatio = estimatedMinutes / remainingMinutes
```

V1 bonus:

| Pressure ratio | Interpretation | Bonus |
| --- | --- | ---: |
| `>= 0.50` | extreme pressure | +30 |
| `>= 0.25` | high pressure | +20 |
| `>= 0.10` | meaningful pressure | +10 |
| `< 0.10` | low clock-time pressure | +0 |

For overdue Tasks, deadline urgency already dominates and no pressure-ratio bonus is needed.

## Important Limitation
This ratio uses total clock time remaining, not the student's actual free hours.

Therefore it is appropriate for relative ranking but must not be presented as a precise probability of finishing on time.

---

# Importance Adjustment

User-selected importance modifies ranking modestly:

| Importance | Adjustment |
| --- | ---: |
| HIGH | +10 |
| NORMAL | +0 |
| not set | +0 |
| LOW | -5 |

Deadline pressure intentionally outweighs importance in most cases.

A user should not be able to mark a distant Task HIGH and automatically suppress an imminent deadline.

---

# Recommendation Score

For an ACTIVE Task:

```text
recommendationScore =
    deadlineBaseScore
    + effortPressureBonus
    + importanceAdjustment
```

The score exists only to rank Tasks.

Do not expose the raw score in the normal product UI.

---

# Deterministic Tie-Breaking

Sort by:

1. higher `recommendationScore`
2. earlier `dueAt`
3. higher effective importance (`HIGH > NORMAL/unset > LOW`)
4. earlier `createdAt`
5. stable UUID lexical order

This guarantees the same input state and same current time produce the same result.

---

# Recommendation Reason

The UI shows one primary human-readable reason plus optional supporting facts.

Reason precedence:

1. `OVERDUE`
2. `INSUFFICIENT_CLOCK_TIME`
3. `DUE_WITHIN_24_HOURS`
4. `HIGH_EFFORT_PRESSURE`
5. `HIGH_IMPORTANCE`
6. `DUE_SOON`
7. `UPCOMING`

Example mappings:

| Code | Example message |
| --- | --- |
| OVERDUE | "เลยกำหนดส่งแล้ว" |
| INSUFFICIENT_CLOCK_TIME | "เวลาที่เหลือน้อยกว่างานที่ประเมินไว้" |
| DUE_WITHIN_24_HOURS | "ใกล้กำหนดส่งมาก" |
| HIGH_EFFORT_PRESSURE | "งานค่อนข้างใหญ่เมื่อเทียบกับเวลาที่เหลือ" |
| HIGH_IMPORTANCE | "ตั้งความสำคัญไว้สูง" |
| DUE_SOON | "กำหนดส่งใกล้เข้ามา" |
| UPCOMING | "เป็นงานถัดไปตามกำหนดส่ง" |

The API should return a reason code and display message separately where practical so wording can evolve without changing the ranking rule.

---

# Start Guidance

V1 does not promise exact hour-by-hour scheduling.

It returns coarse guidance:

```text
START_NOW
START_TODAY
START_SOON
CAN_WAIT
```

Rules:

### START_NOW
Any of:
- Task is overdue
- risk state is `AT_RISK`
- deadline is within 24 hours

### START_TODAY
Any of:
- deadline is within 72 hours
- pressure ratio is `>= 0.25`

### START_SOON
Any of:
- deadline is within 7 days
- importance is HIGH

### CAN_WAIT
- none of the above

Higher guidance wins when multiple conditions apply.

This answers "when should I start?" without pretending JodNgan knows the user's detailed daily availability.

---

# Risk Assessment V1

Risk must be conservative because the MVP does not know the user's actual free-time schedule.

## States

```text
AT_RISK
NO_CONFIRMED_RISK
INSUFFICIENT_DATA
```

## Rules

### Completed Task
No active risk evaluation.

### Overdue Active Task

```text
AT_RISK
reason = OVERDUE
```

The deadline has already been missed.

### Effort Unknown

```text
INSUFFICIENT_DATA
reason = EFFORT_UNKNOWN
```

The UI may still show deadline urgency, but must not claim workload-based risk.

### Effort Known, Future Deadline

If:

```text
estimatedMinutes >= remainingMinutes
```

then:

```text
AT_RISK
reason = INSUFFICIENT_CLOCK_TIME
```

Otherwise:

```text
NO_CONFIRMED_RISK
```

## Why This Rule Is Conservative
If estimated work exceeds all clock time left, the Task is clearly at risk even before considering sleep, classes, commuting, or other Tasks.

If it does not exceed clock time, the MVP does not have enough availability data to make a stronger claim.

Later versions may improve this using explicit availability or calendar integration.

---

# Due-Today Display

"Due today" is a local-calendar concept, separate from recommendation scoring.

A Task is `dueToday` when `dueAt` converted to `User.timeZone` has the same local date as `now` converted to that zone.

This means:
- urgency ranking remains based on exact elapsed time
- UI labels remain intuitive in the user's local day

---

# Worked Examples

Assume current time is fixed for all examples.

| Task | Remaining | Effort | Importance | Score logic | Expected behavior |
| --- | ---: | ---: | --- | --- | --- |
| A | overdue | unknown | unset | 100 | recommend strongly; START_NOW; AT_RISK |
| B | 20h | 6h | HIGH | 80 + 20 + 10 = 110 | strong recommendation; START_NOW |
| C | 48h | unknown | unset | 60 | valid recommendation despite missing effort; START_TODAY; risk unknown |
| D | 4d | 15h | NORMAL | 40 + 10 = 50 | meaningful pressure; START_SOON |
| E | 10d | 2h | HIGH | 20 + 0 + 10 = 30 | lower priority; START_SOON because HIGH importance |
| F | 8d | unknown | LOW | 20 - 5 = 15 | CAN_WAIT |

### Example: effort can change order

Task X:
- due in 4 days
- estimated 1 hour
- NORMAL
- score = 40

Task Y:
- due in 4 days
- estimated 20 hours
- NORMAL
- pressure ratio is meaningful
- score = 50

Y ranks above X even with the same deadline window because the known workload is larger relative to time remaining.

---

# Edge Cases

## Deadline exactly now
Treat as CRITICAL / reached deadline, not future-safe.

## Estimated effort removed
Recommendation immediately falls back to deadline + importance rules.

Risk becomes `INSUFFICIENT_DATA` unless overdue.

## Importance removed
Treat as NORMAL for ranking while preserving `null` as "not explicitly selected" in persisted data.

## Course changes
Course has no effect on V1 recommendation ranking.

## Multiple identical Tasks
Stable tie-breakers ensure deterministic ordering.

## Clock changes during a request
Capture `now` once per scheduling operation and use that same value for every Task in that calculation.

---

# Implementation Boundary

The scheduling engine should be a pure/testable domain service where practical.

Conceptually:

```text
List<SchedulableTask> + PlanningContext(now, zone)
                    │
                    v
             SchedulingEngine
                    │
                    v
PlanningResult
- ranked tasks
- primary recommendation
- reason
- start guidance
- risk assessments
```

No database access belongs inside the ranking algorithm.

No HTTP concerns belong inside the ranking algorithm.

---

# V1 Non-Goals

The first engine does not:
- reserve exact time blocks
- know the student's class timetable
- know sleep hours
- know commute time
- redistribute work across a calendar
- calculate probability of completion
- use AI

Those capabilities require additional user data and should not be simulated with false precision.
