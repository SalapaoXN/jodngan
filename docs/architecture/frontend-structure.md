# Frontend Structure

## Status
Accepted for MVP implementation.

## Stack Direction
- React
- TypeScript
- React Router
- TanStack Query for server state
- React Hook Form + schema validation for forms
- Tailwind CSS / shadcn-style primitives may be added during visual implementation if they reduce boilerplate

Do not add Redux or another global client-state store unless a concrete need appears.

---

# Route Map

```text
/login
/register
/
/tasks
/tasks/:taskId
```

Route meaning after authentication is added:
- `/login` -> Sign In
- `/register` -> Register
- `/` -> Today
- `/tasks` -> Tasks
- `/tasks/:taskId` -> Task Detail

Slice 6 activates all five routes. `/login` and `/register` are public; `/`, `/tasks`, and `/tasks/:taskId` are protected by `AuthGuard`.

Quick Add and Course Management are interaction surfaces rather than primary routes.

---

# Authentication Routing

Slice 6 uses session-aware routing described by ADR 0002 without changing the core Today/Tasks interaction model.

Current startup behavior:
1. bootstrap `GET /api/csrf` into frontend memory
2. query `GET /api/auth/me` when a protected route renders
3. show a short session-loading state while the current User is unknown
4. redirect unauthenticated protected routes to `/login`, preserving the requested internal path
5. after login, return to that internal path when safe; after registration, open Today

Passwords, session IDs, and CSRF tokens are never stored in localStorage/sessionStorage.

---

# Suggested Source Shape

```text
src/
├── app/
│   ├── router/
│   ├── providers/
│   └── App.tsx
│
├── features/
│   ├── auth/
│   ├── courses/
│   ├── tasks/
│   └── today/
│
├── components/
│   └── ui/
│
├── lib/
│   ├── api/
│   ├── time/
│   └── validation/
│
└── main.tsx
```

Prefer feature-oriented code over one giant global `components/`, `hooks/`, and `services/` structure.

---

# Feature Responsibilities

## auth
Owns frontend behavior for:
- Sign In
- Register
- current session query
- logout
- authenticated route guard

Candidate files:

```text
auth/
├── api.ts
├── queries.ts
├── LoginPage.tsx
├── RegisterPage.tsx
└── AuthGuard.tsx
```

---

## tasks
Owns:
- Quick Add
- Tasks screen
- Task Detail
- complete/reopen mutations
- task API types

Candidate files:

```text
tasks/
├── api.ts
├── queries.ts
├── types.ts
├── TaskListPage.tsx
├── TaskDetailPage.tsx
├── QuickAddTask.tsx
├── TaskListItem.tsx
├── RemainingTimeLabel.tsx
└── RiskBadge.tsx
```

---

## courses
Owns:
- Course selector
- lightweight Course management
- Course queries/mutations

Candidate files:

```text
courses/
├── api.ts
├── queries.ts
├── types.ts
├── CourseSelector.tsx
└── CourseManager.tsx
```

No dedicated Course route is required in the first MVP.

---

## today
Owns:
- Today query
- primary recommendation presentation
- upcoming-task presentation

Candidate files:

```text
today/
├── api.ts
├── queries.ts
├── TodayPage.tsx
└── RecommendationCard.tsx
```

Frontend displays recommendation data returned by the backend.

It must not reimplement the scheduling score.

---

# Server State

Use TanStack Query (or equivalent single server-state library) for:
- current user
- Today summary
- Task lists
- Task Detail
- Course list

After mutations, update/invalidate only related queries.

Examples:

## Create Task
Invalidate/update:
- `today`
- active `tasks`

## Update Task
Invalidate/update:
- task detail for ID
- `today`
- active/completed task lists as applicable

## Complete/Reopen
Invalidate/update:
- task detail
- Today
- active Tasks
- completed Tasks

## Course mutation
Invalidate/update:
- Courses
- relevant task/today queries when Course display may change

Avoid maintaining a second manually synchronized copy of server entities in a global client store.

---

# Local UI State

Use component/local state for transient concerns such as:
- Quick Add open/closed
- Course Manager open/closed
- selected Tasks filter tab
- temporary form input before submission

Do not persist these into global state by default.

---

# Quick Add Interaction

## Mobile
Prefer a full-height or near-full-height sheet/dialog that preserves one-hand usability.

Primary order:
1. Task name
2. Due date/time
3. optional Course
4. Save

## Desktop
The same component may render as:
- modal
- side sheet
- compact panel

The data model and submit behavior remain identical.

## Success
After successful creation:
- close Quick Add
- surface the created Task immediately
- refresh Today recommendation
- show remaining-time data returned by the API

---

# Task List

MVP controls:
- Active / Completed
- Course filter
- Quick Add

Each row/card should show:
- title
- Course if present
- deadline/remaining label
- urgency/risk cue
- completion action

Avoid dense tables on phone screens.

---

# Task Detail

Task Detail owns progressive enrichment fields:
- title
- due date/time
- Course
- estimated effort
- importance
- notes

It also displays backend-derived:
- remaining time
- urgency
- start guidance
- risk explanation

The frontend must not calculate a duplicate priority score.

---

# Shared UI Components

Create reusable components only where actual repetition exists.

Likely shared primitives:
- Button
- Input
- Dialog/Sheet
- Select
- FormField
- EmptyState
- ErrorState
- LoadingSkeleton

Feature-level reusable components:
- QuickAddTask
- TaskListItem
- RemainingTimeLabel
- RecommendationCard
- RiskBadge
- CourseSelector

Do not build a custom design system before the actual UI demonstrates repeated patterns.

---

# API Client

Use one small HTTP client wrapper responsible for:
- `/api` base path
- JSON parsing
- CSRF header behavior
- normalized error parsing
- credentials/session cookie behavior

Slice 6 CSRF client behavior:
- fetch `GET /api/csrf` on application initialization
- keep the returned token/header name in memory only
- automatically attach the CSRF header to unsafe methods
- fetch a fresh token after registration, login, and logout
- never store CSRF tokens, session IDs, or passwords in localStorage/sessionStorage

Do not create one generic abstraction layer per endpoint solely for architectural symmetry.

Feature API modules may call the shared client.

---

# Form Validation

Frontend validation improves feedback but is not an authorization/security boundary.

Validate:
- required fields
- obvious input format
- positive effort values

Backend remains authoritative and validates every request again.

Use the same visible constraints as the API where practical:
- Task title max 200 chars
- Course name max 120 chars

---

# Time Handling

When the user chooses a deadline:
1. interpret input in the browser/user local zone
2. convert to an absolute ISO timestamp for API submission
3. display returned deadline in user local time

At registration, detect browser IANA zone using the platform time-zone API and send it to the backend.

The backend-derived `remainingLabel`, `urgency`, and risk data remain authoritative for planning semantics.

---

# Responsive Rules

Design mobile first.

Minimum expectations:
- no horizontal scroll in core screens
- primary actions comfortably tappable
- recommendation readable without opening Task Detail
- Quick Add works on phone viewport
- desktop layouts may increase spacing/width without changing core information hierarchy

---

# Loading / Error / Empty States

Every query-driven screen must explicitly handle:

```text
loading
success with data
success empty
recoverable error
```

Examples:

## Today Empty
Prompt user to add first Task.

## Tasks Empty
Different message for:
- no active tasks
- no completed tasks

## Error
Show concise error and retry when useful.

Do not replace the whole app with a blank screen on a single query failure.

---

# Accessibility Baseline

- semantic buttons/inputs rather than clickable `div`s
- form labels associated with controls
- keyboard operable dialogs and navigation
- visible focus state
- status not communicated by color alone
- accessible error text

---

# Frontend Non-Goals

Do not add initially:
- Redux
- complex global event bus
- frontend scheduling formula
- offline sync engine
- PWA complexity
- custom calendar component
- analytics dashboard

---

# Acceptance Checklist

- [x] Minimal route count.
- [x] Quick Add is not a primary route.
- [x] Server state has one source of truth.
- [x] React does not duplicate scheduling logic.
- [x] Mobile flow remains primary.
- [x] Loading/error/empty states are explicit.
- [x] Feature folders map clearly to product capabilities.
