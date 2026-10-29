# REST API Contract

## Status
Target MVP contract accepted. Slice 6 implements session authentication, CSRF protection, and per-user Task/Course ownership on top of the core planning API.

## Base Path

```text
/api
```

No `/v1` prefix is used for the internal MVP API because external backwards-compatible API versioning is not currently a requirement.

---

# General Conventions

## Content Type

```text
Content-Type: application/json
```

## Authentication
`GET /api/csrf`, `POST /api/auth/register`, and `POST /api/auth/login` are public. Other `/api/**` endpoints require the Spring Security session established by login/registration.

The browser receives an HTTP-only session cookie. Unsafe requests use Spring Security CSRF protection and must include the current token header returned by `/api/csrf`.

## Time
API timestamps use ISO-8601 absolute timestamps.

Example:

```json
"2026-10-31T16:59:00Z"
```

The frontend converts the user's local date/time selection into an absolute timestamp before submission.

## User-Owned Resources
Resource lookup includes the authenticated User ownership boundary. When a Task/Course exists but belongs to another User, respond as not found rather than revealing resource existence. Course assignment also validates that the Course belongs to the same authenticated User.

---

# Error Shape

All application errors should use one consistent envelope.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": {
    "title": "Task name is required"
  },
  "requestId": "optional-correlation-id"
}
```

`fieldErrors` may be omitted when not relevant.

## Common Codes

```text
VALIDATION_ERROR
UNAUTHENTICATED
FORBIDDEN
INVALID_CREDENTIALS
RESOURCE_NOT_FOUND
EMAIL_ALREADY_EXISTS
CONFLICT
INTERNAL_ERROR
```

Do not return stack traces, password hashes, SQL details, or secrets.

---

# Authentication (Slice 6)

## GET /api/csrf
Return the current Spring Security CSRF token metadata for the browser client.

This endpoint is public because registration and login are CSRF-protected state-changing requests.

### Response - 200

```json
{
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "server-generated-token"
}
```

The frontend keeps the token in memory and sends it using `headerName` on unsafe requests.

Fetch a fresh token on application initialization and again after successful registration, login, or logout because authentication/logout may clear the previous CSRF token.

---

## POST /api/auth/register
Create an account and establish an authenticated session.

### Request

```json
{
  "email": "student@example.com",
  "password": "user-entered-password",
  "timeZone": "Asia/Bangkok"
}
```

`timeZone` is normally detected by the frontend rather than typed by the user.

### Response - 201

```json
{
  "user": {
    "id": "d86d8dcb-f7df-4af2-9a56-a953d4fa1995",
    "email": "student@example.com",
    "timeZone": "Asia/Bangkok"
  }
}
```

### Errors
- `400 VALIDATION_ERROR`
- `409 EMAIL_ALREADY_EXISTS`

---

## POST /api/auth/login
Authenticate and establish a session.

### Request

```json
{
  "email": "student@example.com",
  "password": "user-entered-password"
}
```

### Response - 200

```json
{
  "user": {
    "id": "d86d8dcb-f7df-4af2-9a56-a953d4fa1995",
    "email": "student@example.com",
    "timeZone": "Asia/Bangkok"
  }
}
```

### Errors
- `400 VALIDATION_ERROR`
- `401 INVALID_CREDENTIALS`

Use a generic invalid-credentials response; do not reveal whether the email exists.

---

## POST /api/auth/logout
Invalidate the authenticated session.

### Response - 204
No response body.

---

## GET /api/auth/me
Return the current authenticated user.

### Response - 200

```json
{
  "user": {
    "id": "d86d8dcb-f7df-4af2-9a56-a953d4fa1995",
    "email": "student@example.com",
    "timeZone": "Asia/Bangkok"
  }
}
```

### Errors
- `401 UNAUTHENTICATED`

---

# Task Representation

## Current Slice 6 Task
The authenticated API returns capture, lifecycle, optional Course/planning enrichment, and conservative risk data scoped to the current User:

```json
{
  "id": "7c40f037-9d59-4b54-839f-b324e92f3b8e",
  "title": "Database Assignment",
  "dueAt": "2026-10-31T16:59:00Z",
  "course": {
    "id": "933fe6fd-55bc-447a-a40c-eaff32e24f67",
    "name": "Data Warehouse"
  },
  "estimatedMinutes": 180,
  "importance": "HIGH",
  "notes": "Split the report into sections",
  "status": "ACTIVE",
  "completedAt": null,
  "createdAt": "2026-10-28T14:00:00Z",
  "updatedAt": "2026-10-28T14:00:00Z",
  "planning": {
    "remainingSeconds": 266400,
    "overdue": false,
    "risk": {
      "state": "NO_CONFIRMED_RISK",
      "reasonCode": null,
      "message": null
    }
  }
}
```

`remainingSeconds`, `overdue`, and `risk` are derived and are not persisted. `estimatedMinutes`, `importance`, and `notes` are nullable and may be cleared by sending `null` (or omitting them from the current full-replacement update payload). Completed Tasks return no active risk assessment.

`course` is nullable. Task create/update accepts nullable `courseId`; a supplied ID must reference a Course owned by the authenticated User.

## Task Summary
Used by Today and Tasks lists.

```json
{
  "id": "7c40f037-9d59-4b54-839f-b324e92f3b8e",
  "title": "Database Assignment",
  "dueAt": "2026-10-31T16:59:00Z",
  "course": {
    "id": "933fe6fd-55bc-447a-a40c-eaff32e24f67",
    "name": "Data Warehouse"
  },
  "status": "ACTIVE",
  "estimatedMinutes": 360,
  "importance": "HIGH",
  "planning": {
    "remainingSeconds": 72000,
    "remainingLabel": "เหลือ 20 ชั่วโมง",
    "dueToday": false,
    "overdue": false,
    "urgency": "CRITICAL",
    "startGuidance": "START_NOW",
    "risk": {
      "state": "NO_CONFIRMED_RISK",
      "reasonCode": null,
      "message": null
    }
  }
}
```

`course`, `estimatedMinutes`, and `importance` may be null.

## Nullable Task Detail Example
The same current representation supports unassigned Tasks and missing optional planning fields:

```json
{
  "id": "7c40f037-9d59-4b54-839f-b324e92f3b8e",
  "title": "Database Assignment",
  "dueAt": "2026-10-31T16:59:00Z",
  "course": null,
  "estimatedMinutes": null,
  "importance": null,
  "notes": null,
  "status": "ACTIVE",
  "completedAt": null,
  "createdAt": "2026-10-28T14:00:00Z",
  "updatedAt": "2026-10-28T14:00:00Z",
  "planning": {
    "remainingSeconds": 266400,
    "remainingLabel": "เหลือ 3 วัน",
    "dueToday": false,
    "overdue": false,
    "urgency": "HIGH",
    "startGuidance": "START_TODAY",
    "risk": {
      "state": "INSUFFICIENT_DATA",
      "reasonCode": "EFFORT_UNKNOWN",
      "message": "เพิ่มเวลาที่คาดว่าจะใช้เพื่อประเมินความเสี่ยงได้ดีขึ้น"
    }
  }
}
```

---

# Tasks

## POST /api/tasks
Quick Add endpoint.

### Request - Current Slice 6

```json
{
  "title": "Database Assignment",
  "dueAt": "2026-10-31T16:59:00Z",
  "courseId": "933fe6fd-55bc-447a-a40c-eaff32e24f67"
}
```

`courseId` is optional and may be `null` or omitted. Quick Add still does not require estimated effort, importance, or notes, but the endpoint requires an authenticated session.

### Response - 201
Returns the current Task representation with derived remaining-time data immediately.

### Errors - Current Slice 6
- `400 VALIDATION_ERROR`
- `401 UNAUTHENTICATED`
- `404 RESOURCE_NOT_FOUND` when a supplied `courseId` is missing or belongs to another User

---

## GET /api/tasks
List Tasks owned by the authenticated User.

### Query Parameters
Slice 6 supports lifecycle and optional Course filtering within the authenticated User's data:

```text
status=ACTIVE|COMPLETED   optional, defaults to ACTIVE
courseId=UUID             optional
```

### Response - 200

```json
{
  "items": [
    {
      "id": "7c40f037-9d59-4b54-839f-b324e92f3b8e",
      "title": "Database Assignment",
      "dueAt": "2026-10-31T16:59:00Z",
      "status": "ACTIVE",
      "completedAt": null,
      "createdAt": "2026-10-28T14:00:00Z",
      "updatedAt": "2026-10-28T14:00:00Z",
      "planning": {
        "remainingSeconds": 266400,
        "overdue": false
      }
    }
  ]
}
```

The current Slice 6 Task list uses the same Task representation, scoped to the authenticated User, including nullable Course summary, effort, importance, notes, and conservative risk. Due-today display enrichment remains separate from this endpoint.

Ordering:
- ACTIVE Tasks by due date ascending, then stable ID ordering
- COMPLETED Tasks by completed date descending, then stable ID ordering

This endpoint is a Task list, not the scheduling ranking endpoint.

---

## GET /api/tasks/{taskId}
Return one Task Detail using the current Slice 6 representation above, including `completedAt`, nullable Course summary, optional effort/importance/notes, and derived risk.

### Response - 200
Current Task representation.

### Errors - Current Slice 6
- `401 UNAUTHENTICATED`
- `404 RESOURCE_NOT_FOUND` for missing or cross-user Task

---

## PUT /api/tasks/{taskId} (Slice 6)
Edit core Task fields, optional Course assignment, and optional planning enrichment from Task Detail.

### Request - Current Slice 6

```json
{
  "title": "Database Final Assignment",
  "dueAt": "2026-11-01T16:59:00Z",
  "courseId": "933fe6fd-55bc-447a-a40c-eaff32e24f67",
  "estimatedMinutes": 180,
  "importance": "HIGH",
  "notes": "Split the report into sections"
}
```

`courseId`, `estimatedMinutes`, `importance`, and `notes` are optional. Sending `courseId: null` clears Course assignment. Sending the planning fields as `null` (or omitting them from this full-replacement request) clears their stored value.

### Response - 200
Returns the updated current Task representation with recalculated deadline, recommendation input, and risk data.

### Errors - Current Slice 6
- `400 VALIDATION_ERROR`
- `401 UNAUTHENTICATED`
- `404 RESOURCE_NOT_FOUND` for missing/cross-user Task or Course assignment

---

## POST /api/tasks/{taskId}/complete (Slice 3)
Mark Task complete.

The operation is idempotent from the client's point of view. Completing an already completed Task returns its current completed state. `completedAt` is recorded on the first transition to COMPLETED.

### Response - 200
Returns updated Task Detail.

---

## POST /api/tasks/{taskId}/reopen (Slice 3)
Return Task to ACTIVE state and clear `completedAt`.

The operation is idempotent from the client's point of view.

### Response - 200
Returns updated Task Detail with recalculated planning data.

---

# Today / Recommendation (Slice 2)

## GET /api/today
Return the data required by the Today screen in one request.

### Response - 200

```json
{
  "generatedAt": "2026-10-29T01:00:00Z",
  "primaryRecommendation": {
    "task": {
      "id": "7c40f037-9d59-4b54-839f-b324e92f3b8e",
      "title": "Database Assignment",
      "dueAt": "2026-10-31T16:59:00Z",
      "course": {
        "id": "933fe6fd-55bc-447a-a40c-eaff32e24f67",
        "name": "Data Warehouse"
      },
      "status": "ACTIVE",
      "createdAt": "2026-10-28T14:00:00Z",
      "updatedAt": "2026-10-28T14:00:00Z",
      "planning": {
        "remainingSeconds": 230340,
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

The Today response is scoped to the authenticated User and enriched with optional Course, effort, importance, and risk data. Course is display context only and does not affect recommendation scoring or risk. Recommendation ordering remains backend-owned.

If no active Tasks exist:

```json
{
  "generatedAt": "2026-10-28T15:00:00Z",
  "primaryRecommendation": null,
  "upcomingTasks": []
}
```

## Rules
- Backend owns recommendation ordering.
- Frontend must not recompute the recommendation score.
- `generatedAt` identifies the time used for the calculation.
- `upcomingTasks` contains the next ranked Tasks after the primary recommendation and is capped at 5.
- Each recommendation item carries backend-derived `urgency`, `reasonCode`, `reason`, and `startGuidance`; React displays these values and does not rerank Tasks.

---

# Courses (Slice 5)

## Course Representation

```json
{
  "id": "933fe6fd-55bc-447a-a40c-eaff32e24f67",
  "name": "Data Warehouse"
}
```

---

## POST /api/courses
Create a Course.

### Request

```json
{
  "name": "Data Warehouse"
}
```

### Response - 201
Returns Course representation.

### Errors
- `400 VALIDATION_ERROR`
- `401 UNAUTHENTICATED`

---

## GET /api/courses
Return Courses owned by the authenticated User.

### Response - 200

```json
{
  "items": [
    {
      "id": "933fe6fd-55bc-447a-a40c-eaff32e24f67",
      "name": "Data Warehouse"
    }
  ]
}
```

Ordering: name ascending, then stable ID ordering.

---

## PUT /api/courses/{courseId}
Rename a Course.

### Request

```json
{
  "name": "Data Warehouse & Cloud"
}
```

### Response - 200
Returns updated Course.

### Errors
- `400 VALIDATION_ERROR`
- `404 RESOURCE_NOT_FOUND`

---

## DELETE /api/courses/{courseId}
Remove a Course.

Linked Tasks remain and become unassigned. The database foreign key uses `ON DELETE SET NULL` as the structural guarantee.

### Response - 204
No response body.

### Errors
- `404 RESOURCE_NOT_FOUND`

---

# Validation Rules at API Boundary

## Task
- title required after trim
- title max 200 characters
- dueAt required and parseable
- estimatedMinutes must be greater than zero when supplied
- importance must be LOW, NORMAL, HIGH, or null
- courseId must reference a Course owned by the authenticated User

Creating a Task whose deadline is already in the past is allowed.

Reason: students may record already-overdue work and need JodNgan to surface it immediately.

## Course
- name required after trim
- max 120 characters

## Registration
- syntactically valid email
- password required with a minimum of 8 characters
- no arbitrary uppercase/digit/symbol composition requirement
- valid IANA time-zone identifier

---

# HTTP Status Summary

| Situation | Status |
| --- | ---: |
| successful read/update/action | 200 |
| resource created | 201 |
| successful no-content action | 204 |
| request validation failed | 400 |
| not authenticated / invalid login | 401 |
| owned resource not found or inaccessible | 404 |
| duplicate email / explicit conflict | 409 |
| unexpected server error | 500 |

---

# API Non-Goals for MVP

Not included:
- task hard-delete endpoint
- calendar sync endpoints
- Classroom/Teams endpoints
- notification endpoints
- AI command endpoint
- bulk task import
- public API keys
- external API version guarantees

---

# Contract Acceptance Checklist

- [x] Quick Add payload is minimal.
- [x] Optional planning fields can be added/cleared later.
- [x] Scheduling logic remains server-side.
- [x] Today can render from one endpoint.
- [x] User ownership is enforced for Task/Course resources and cross-user access is hidden as not found.
- [x] Task complete/reopen are explicit and idempotent.
- [x] Course removal preserves Tasks.
- [x] Error shape is consistent.
