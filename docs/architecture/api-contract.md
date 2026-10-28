# REST API Contract

## Status
Target MVP contract accepted. Current implementation is the single-user core trial; authentication/ownership and later Task/Course capabilities remain documented below but are deferred until their slices.

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
Current trial mode is single-user and does not require authentication. Core Task/Today endpoints are usable without Sign In/Register while the app is being validated privately.

When the authentication slice is implemented, authenticated endpoints use the Spring Security session established by login/registration, the browser receives an HTTP-only session cookie, and unsafe requests use Spring Security CSRF protection.

The unauthenticated trial configuration is not intended for direct public-internet exposure.

## Time
API timestamps use ISO-8601 absolute timestamps.

Example:

```json
"2026-10-31T16:59:00Z"
```

The frontend converts the user's local date/time selection into an absolute timestamp before submission.

## User-Owned Resources
Ownership is deferred during the single-user trial.

After authentication is introduced, resource lookup always includes the authenticated User ownership boundary. When a Task/Course exists but belongs to another User, respond as not found rather than revealing resource existence.

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
INVALID_CREDENTIALS
RESOURCE_NOT_FOUND
EMAIL_ALREADY_EXISTS
CONFLICT
INTERNAL_ERROR
```

Do not return stack traces, password hashes, SQL details, or secrets.

---

# Authentication (Deferred Until Core Flow Is Complete)

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

## Current Slice 1 Task
The single-user trial initially returns only fields needed for capture and deadline feedback:

```json
{
  "id": "7c40f037-9d59-4b54-839f-b324e92f3b8e",
  "title": "Database Assignment",
  "dueAt": "2026-10-31T16:59:00Z",
  "status": "ACTIVE",
  "createdAt": "2026-10-28T14:00:00Z",
  "updatedAt": "2026-10-28T14:00:00Z",
  "planning": {
    "remainingSeconds": 266400,
    "overdue": false
  }
}
```

`remainingSeconds` and `overdue` are derived and are not persisted.

The richer representations below are target shapes introduced progressively by later slices.

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

## Task Detail
Adds editable fields:

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

### Request - Current Slice 1

```json
{
  "title": "Database Assignment",
  "dueAt": "2026-10-31T16:59:00Z"
}
```

Current Quick Add does not require Course, estimated effort, importance, notes, or authentication.

`courseId` is added later when the Course slice is implemented.

### Response - 201
Returns the current Task representation with derived remaining-time data immediately.

### Errors - Current Slice 1
- `400 VALIDATION_ERROR`

---

## GET /api/tasks
List Tasks in the current single-user dataset. After authentication is introduced, this becomes ownership-scoped to the authenticated User.

### Query Parameters
Current Slice 1 has no required query parameters and returns ACTIVE Tasks only.

Later slices add:

```text
status=ACTIVE|COMPLETED
courseId=<uuid>       optional
```

### Response - 200

```json
{
  "items": [
    {
      "id": "7c40f037-9d59-4b54-839f-b324e92f3b8e",
      "title": "Database Assignment",
      "dueAt": "2026-10-31T16:59:00Z",
      "course": null,
      "status": "ACTIVE",
      "estimatedMinutes": null,
      "importance": null,
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
          "message": null
        }
      }
    }
  ]
}
```

Ordering:
- ACTIVE: due date ascending for browsing
- COMPLETED: completed date descending

This endpoint is a Task list, not the scheduling ranking endpoint.

---

## GET /api/tasks/{taskId}
Return one Task Detail.

### Response - 200
Task Detail representation.

### Errors - Current Slice 1
- `404 RESOURCE_NOT_FOUND`

Authentication adds `401 UNAUTHENTICATED` later.

---

## PUT /api/tasks/{taskId} (Later Slice)
Replace the editable Task fields from Task Detail.

### Request

```json
{
  "title": "Database Final Assignment",
  "dueAt": "2026-11-01T16:59:00Z",
  "courseId": null,
  "estimatedMinutes": 360,
  "importance": "HIGH",
  "notes": "Finish schema and SQL section"
}
```

Explicit null clears an optional field.

### Response - 200
Returns updated Task Detail with recalculated planning data.

### Errors
- `400 VALIDATION_ERROR`
- `404 RESOURCE_NOT_FOUND`
- `401 UNAUTHENTICATED`

---

## POST /api/tasks/{taskId}/complete (Later Slice)
Mark Task complete.

The operation is idempotent from the client's point of view. Completing an already completed owned Task returns its current completed state.

### Response - 200
Returns updated Task Detail.

---

## POST /api/tasks/{taskId}/reopen (Later Slice)
Return Task to ACTIVE state.

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
  "generatedAt": "2026-10-28T15:00:00Z",
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
    },
    "reasonCode": "DUE_WITHIN_24_HOURS",
    "reason": "ใกล้กำหนดส่งมาก"
  },
  "upcomingTasks": []
}
```

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
- Upcoming list should be intentionally short for the Today UI; initial implementation may cap it at 5.

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
Return Courses owned by current User.

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

Suggested ordering: name ascending.

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

Linked Tasks remain and become unassigned.

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
- courseId must reference an existing Course; ownership is additionally enforced after authentication is introduced

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
| not authenticated / invalid login (after authentication slice) | 401 |
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
- [x] User ownership is defined for the later authentication slice; single-user trial intentionally has no ownership boundary.
- [x] Task complete/reopen are explicit and idempotent.
- [x] Course removal preserves Tasks.
- [x] Error shape is consistent.
