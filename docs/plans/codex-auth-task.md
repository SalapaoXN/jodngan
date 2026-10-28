# Codex Task: Implement Slice 1 Authentication

## Goal
Implement JodNgan Slice 1 end to end: real application-owned email/password authentication with PostgreSQL persistence, Spring Security server sessions, CSRF protection, and React authentication screens.

Do not start Task, Course, Today, scheduling, integrations, notifications, OAuth, JWT, password reset, email verification, or MFA work.

---

## Read First
- `AGENTS.md`
- `PLANS.md`
- `docs/plans/vertical-slices.md`
- `docs/architecture/domain-model.md`
- `docs/architecture/database.md`
- `docs/architecture/api-contract.md`
- `docs/architecture/module-boundaries.md`
- `docs/architecture/frontend-structure.md`
- `docs/architecture/testing-strategy.md`
- `docs/architecture/decisions/0002-session-authentication.md`

Slice 0 is complete and verified. Preserve the existing foundation and work only inside Slice 1 scope.

---

# Backend

## 1. Database Migration
Create:

```text
backend/src/main/resources/db/migration/V1__create_users.sql
```

Create `users` exactly from the accepted database design:
- `id uuid primary key`
- `email varchar(320) not null unique`
- `password_hash varchar(255) not null`
- `time_zone varchar(64) not null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

Do not create Course or Task tables in this slice.

Flyway migration must apply successfully from an empty PostgreSQL database.

## 2. identity Module
Implement the identity module under the agreed modular-monolith boundary:

```text
com.jodngan.identity
├── api
├── application
├── domain
└── infrastructure
```

Do not expose password hashes outside the identity module.

Implement only what Slice 1 requires:
- User persistence
- normalized email identity
- password encoding
- registration
- login
- logout
- current authenticated user
- authenticated principal/user context

Email normalization:
- trim surrounding whitespace
- lowercase using locale-independent behavior
- persist only the normalized value

Time zone:
- validate as a real IANA time-zone ID
- reject invalid values with `400 VALIDATION_ERROR`

## 3. Password Handling
Use Spring Security `PasswordEncoder` support; no custom cryptography and no plaintext storage.

Prefer the maintained Spring Security password encoder factory / delegating encoder rather than inventing an encoding format.

Password policy for the MVP:
- required
- minimum 8 characters
- no arbitrary uppercase/digit/symbol composition rules
- never silently truncate a password
- encoder/validation failures must become a safe validation response rather than an internal error

Never log request passwords or password hashes.

## 4. Session Authentication
Use server-managed Spring Security sessions as accepted in ADR 0002.

Implement JSON endpoints from the API contract:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

Registration:
- validate request
- normalize email
- reject duplicate normalized email with `409 EMAIL_ALREADY_EXISTS`
- encode password before persistence
- create User
- authenticate the new user immediately
- persist the Spring Security context into the HTTP session
- return `201` with the safe User representation

Login:
- authenticate using Spring Security authentication mechanisms
- return one generic `401 INVALID_CREDENTIALS` for unknown email and wrong password
- do not reveal account existence
- persist authentication in the HTTP session so `/api/auth/me` works on later requests

Important: if login/registration authentication is performed in a custom JSON controller/service rather than a built-in authentication filter, explicitly persist the resulting `SecurityContext` using the appropriate `SecurityContextRepository`. Do not assume setting `SecurityContextHolder` alone will persist the login across requests.

Logout:
- invalidate authentication/session using Spring Security logout support
- return `204`
- clear authentication state correctly

Do not add JWT, bearer tokens, refresh tokens, or localStorage session state.

## 5. CSRF
CSRF remains enabled for unsafe browser requests.

Add a public token endpoint:

```text
GET /api/csrf
```

It should return the Spring Security `CsrfToken` data needed by the frontend, including token and header name.

The frontend must call this endpoint:
- during app initialization before unsafe requests
- after successful registration
- after successful login
- after logout

This is required because Spring Security rotates/clears the previous CSRF token around authentication/logout transitions.

Unsafe requests without a valid CSRF token must be rejected.

Do not globally disable CSRF to make tests or frontend integration easier.

## 6. Security Configuration
Configure the API as a JSON backend for the SPA.

Public:
- `/api/health`
- `GET /api/csrf`
- `POST /api/auth/register` only when valid CSRF is supplied
- `POST /api/auth/login` only when valid CSRF is supplied

Authenticated:
- `/api/auth/me`
- `/api/auth/logout`
- future `/api/**` endpoints by default

Do not rely on form-login redirects or browser HTML login pages.

Return JSON `401 UNAUTHENTICATED` for unauthenticated protected API access.

Keep default session-fixation protection enabled.

## 7. Error Contract
Use the accepted API error envelope:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": {
    "email": "..."
  }
}
```

Support at minimum:
- `VALIDATION_ERROR`
- `UNAUTHENTICATED`
- `INVALID_CREDENTIALS`
- `EMAIL_ALREADY_EXISTS`
- `INTERNAL_ERROR`

Do not leak stack traces, SQL details, password hashes, or framework exception text.

---

# Frontend

## 1. Auth Feature
Create a feature-oriented auth area, approximately:

```text
src/features/auth/
├── api.ts
├── queries.ts
├── types.ts
├── SignInPage.tsx
├── RegisterPage.tsx
└── AuthGuard.tsx
```

Adjust structure only when there is a concrete simplification; do not create speculative abstractions.

## 2. Routes
Implement:
- `/login`
- `/register`
- `/` as an authenticated placeholder/Today shell for now

Do not implement real Task/Today product behavior yet.

Startup behavior:
1. obtain CSRF token
2. request `/api/auth/me`
3. show an app-level loading state while session status is unknown
4. unauthenticated protected route -> `/login`
5. authenticated user visiting `/login` or `/register` -> `/`

## 3. Sign In
Fields:
- email
- password

Behavior:
- client-side required/obvious format validation
- submit through shared API client
- display generic invalid-credentials error
- on success refresh current-user state and navigate to `/`

## 4. Register
Fields:
- email
- password

Do not show a manual time-zone field.

Detect browser IANA time zone using the platform API and include it in registration.

Behavior:
- validate required fields
- password minimum 8 characters
- submit email/password/timeZone
- show duplicate-email and validation errors cleanly
- registration success is already authenticated and navigates to `/`

## 5. Logout
Provide minimal logout behavior on the authenticated placeholder shell.

On logout:
- send CSRF-protected `POST /api/auth/logout`
- clear/invalidate current-user query state
- obtain a fresh CSRF token
- navigate to `/login`

## 6. Shared API Client
Keep `credentials: 'include'`.

Add CSRF handling centrally rather than duplicating it in every mutation:
- obtain token/header name from `/api/csrf`
- keep the current token in memory, not localStorage/sessionStorage
- attach the current CSRF header automatically for unsafe HTTP methods
- refresh token after register/login/logout
- normalize API error envelopes

Do not store passwords, session IDs, or CSRF tokens in browser persistent storage.

---

# Tests

## Backend Integration / API Tests
Use PostgreSQL/Testcontainers where persistence/security behavior matters.

Required cases:
1. V1 migration applies from empty PostgreSQL.
2. Registration persists normalized email.
3. Password stored value is not plaintext and matches via configured `PasswordEncoder`.
4. Duplicate email is rejected, including case/whitespace-normalized duplicates.
5. Invalid email rejected.
6. Invalid IANA time zone rejected.
7. Too-short password rejected.
8. Registration establishes a session.
9. Valid login establishes a session.
10. Unknown email and wrong password return the same generic invalid-credentials contract.
11. `/api/auth/me` rejects unauthenticated requests.
12. `/api/auth/me` returns current safe User for authenticated session.
13. Session survives a subsequent normal request using the same session.
14. Logout invalidates authentication.
15. Unsafe request without CSRF fails.
16. Unsafe request with valid CSRF succeeds.
17. Password hash never appears in API responses/errors.

Use Spring Security test support / MockMvc where appropriate.

## Frontend Tests
At minimum cover:
- app loading while auth state is unknown
- unauthenticated route redirects to `/login`
- authenticated user redirects away from `/login`
- register submits browser time zone
- login invalid-credentials message
- successful login updates auth state
- logout returns to `/login`
- unsafe API mutation receives CSRF header

Prefer testing user-visible behavior and the real API client boundary rather than mocking every hook internally.

---

# Verification
Run and fix failures for all relevant checks:

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

Where practical, manually verify through the development proxy:

```text
register -> authenticated -> refresh -> still authenticated -> logout -> protected route redirects to login
```

Inspect the final Git diff and `git diff --check`.

Do not stage, commit, or push unless explicitly asked.

---

# Definition of Done
Slice 1 is complete only when:
- a real User row is persisted by Flyway-backed PostgreSQL
- passwords are securely encoded
- register/login/logout/me conform to the API contract
- authentication persists through the server session
- CSRF remains enabled and works from the React client
- Sign In/Register/AuthGuard/logout work
- required security tests pass
- backend and frontend verification commands pass
- no Task/Course/Scheduling implementation is introduced

---

# Final Report
Report only:
1. files/features implemented
2. migration/security/session/CSRF approach
3. backend/frontend test and build results
4. manual auth flow result if run
5. unresolved blocker, if any

If the existing docs conflict with an implementation requirement, stop and report the conflict instead of silently changing architecture.
