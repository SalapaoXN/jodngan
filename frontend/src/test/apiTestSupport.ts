import { vi } from 'vitest';

export const authenticatedUser = {
  id: '00000000-0000-0000-0000-000000000099',
  email: 'test@example.com',
  timeZone: 'Asia/Bangkok',
};

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

export function mockAuthenticatedApi(
  handler: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response> | Response,
) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const path = String(input);
    if (path === '/api/auth/me') {
      return jsonResponse({ user: authenticatedUser });
    }
    if (path === '/api/csrf') {
      return jsonResponse({
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
        token: 'test-csrf-token',
      });
    }
    return handler(input, init);
  });
}
