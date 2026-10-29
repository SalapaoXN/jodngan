# Slice 6 - Authentication + Ownership Implementation Task

## Status
In progress.

## Goal
Add production-oriented account security and per-user isolation without changing the proven post-login Today/Tasks workflow.

## Accepted Security Model
- Application-owned email/password authentication.
- Spring Security server-managed session; no JWT.
- Passwords encoded with Spring Security `PasswordEncoder` through an application port.
- CSRF remains enabled for unsafe browser requests.
- Public `GET /api/csrf`; React keeps the token in memory only and refreshes it after register/login/logout.
- Registration establishes an authenticated session immediately.
- Cross-user Task/Course access returns 404 rather than revealing existence.

## Data Migration
V4 intentionally resets pre-auth private-trial Tasks/Courses before ownership becomes mandatory. This avoids a hidden fake owner and allows `user_id NOT NULL` from the first authenticated schema. The reset is acceptable for the current private development trial and must be documented rather than hidden.

## Backend Scope
- Add `users` persistence with normalized unique email, password hash, IANA time zone, and timestamps.
- Add non-null `user_id` ownership to Courses and Tasks.
- Add owner-scoped repository methods; remove unscoped resource lookups from application flows.
- Validate assigned Courses through `(userId, courseId)`.
- Scope Today planning data to the authenticated user.
- Add `GET /api/csrf`.
- Add register/login/logout/current-user APIs.
- Return consistent unauthenticated/invalid-credentials/duplicate-email responses.
- Keep password/session/CSRF values out of logs and API responses.

## Frontend Scope
- Add `/login` and `/register`.
- Add session-aware `AuthGuard` for `/`, `/tasks`, and `/tasks/:taskId`.
- Fetch CSRF at application startup and attach its returned header to unsafe requests.
- Refresh CSRF after registration, login, and logout.
- Detect the browser IANA time zone during registration.
- Add a compact signed-in identity/logout affordance without adding friction to Quick Add.
- Do not store passwords, session IDs, or CSRF tokens in localStorage/sessionStorage.

## Verification
- [ ] Backend: `mvn test`
- [ ] Frontend: `npm run typecheck`
- [ ] Frontend: `npm run lint`
- [ ] Frontend: `npm test`
- [ ] Frontend: `npm run build`
- [ ] `git diff --check`
- [ ] Manual authentication/ownership smoke

## Required Security Tests
- password is never stored plaintext
- normalized duplicate email -> 409
- invalid login -> generic 401
- unauthenticated protected request -> 401
- unsafe request without CSRF -> 403
- registration/login establishes a reusable session
- logout invalidates the session
- User A cannot read/update/complete/filter through User B's Task/Course IDs
- cross-user resources return 404

## Non-Goals
- password reset
- email verification
- OAuth/social login
- MFA
- JWT
- account deletion
- public deployment configuration

## Done When
Authentication works end to end, every Task/Course operation is owner-scoped, CSRF remains enforced, existing planning behavior still works after login, automated verification passes, and manual two-user isolation smoke passes.
