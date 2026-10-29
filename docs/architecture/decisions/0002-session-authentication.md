# ADR 0002: Use Application-Owned Email/Password with Server Sessions

## Status
Accepted and implemented in Slice 6.

## Context
JodNgan first validated the core planning flow as a private single-user trial. Slice 6 now adds per-user Task and Course isolation without changing the proven capture and planning interactions.

The frontend is a browser SPA and the backend is a Spring Boot application.

## Decision
Use application-owned email/password authentication implemented with Spring Security and server-managed sessions. Slice 6 activates Sign In/Register and protects the core application routes.

Key choices:
- Email is the login identifier.
- Passwords are encoded with Spring Security's established `PasswordEncoder` support; no custom cryptography.
- Authentication state is stored in the server session.
- The browser receives a secure HTTP-only session cookie.
- CSRF protection remains enabled for state-changing browser requests.
- Registration establishes an authenticated session immediately on success.

## Why Sessions Instead of JWT for MVP
JodNgan does not currently need:
- third-party API clients
- mobile native clients
- cross-service token propagation
- stateless microservices

A server session is simpler for the current browser + monolith architecture and avoids adding token refresh/rotation logic with no product benefit.

## Password Handling
- Never store plaintext passwords.
- Never log passwords.
- Use Spring Security's maintained password encoding mechanism.
- Password policy should be practical rather than inventing custom cryptographic requirements.

## Authorization
Authentication alone is not enough.

Every Task and Course operation must also enforce ownership using the authenticated User ID.

Cross-user resources should be treated as not found at the API boundary to avoid leaking their existence.

## Session Cookie
Production configuration should use appropriate cookie protections such as:
- HttpOnly
- Secure when served over HTTPS
- sensible SameSite behavior

Exact deployment cookie configuration may differ between local development and production.

## CSRF
Because authentication uses browser cookies, CSRF protection remains enabled.

The React client must use the Spring Security CSRF token mechanism for unsafe requests.

For the SPA, expose a public `GET /api/csrf` token endpoint, keep the token in frontend memory only, attach its returned header on unsafe requests, and fetch a fresh token after registration, login, and logout.

Do not solve integration inconvenience by disabling CSRF globally.

## Registration Time Zone
Registration may include a browser-detected IANA time-zone identifier as non-secret account metadata.

This supports server-side deadline labels such as "due today" without adding a visible setup step.

## Consequences

### Positive
- Simple browser authentication model.
- Demonstrates Spring Security correctly.
- No OAuth provider setup required for the core MVP.
- No custom JWT/refresh-token subsystem.
- Session invalidation is straightforward.

### Trade-offs
- Backend keeps session state.
- CSRF must be handled correctly by the SPA.
- Native/mobile clients would likely need a different auth strategy later.
- Password reset/email verification are additional concerns if the product becomes public-facing.

## MVP Non-Goals
Not required initially:
- password reset flow
- email verification
- social login
- Google OAuth
- Microsoft OAuth
- MFA
- JWT access/refresh tokens

These may be reconsidered when there is a real product or deployment requirement.

## Revisit When
Reconsider the authentication strategy if JodNgan gains:
- native mobile clients
- public third-party API access
- independently deployed backend services
- external identity-provider requirements
- institution-managed SSO
