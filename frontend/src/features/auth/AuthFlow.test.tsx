import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from '../../app/App';
import { AppProviders } from '../../app/providers/AppProviders';
import { authenticatedUser, jsonResponse } from '../../test/apiTestSupport';

function renderApp(path: string) {
  window.history.pushState({}, '', path);
  return render(
    <AppProviders>
      <App />
    </AppProviders>,
  );
}

function csrfResponse(): Response {
  return jsonResponse({
    headerName: 'X-CSRF-TOKEN',
    parameterName: '_csrf',
    token: 'auth-flow-csrf',
  });
}

function emptyTodayResponse() {
  return {
    generatedAt: '2030-03-01T00:00:00Z',
    primaryRecommendation: null,
    upcomingTasks: [],
  };
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  window.history.pushState({}, '', '/');
});

describe('authentication flow', () => {
  it('redirects protected routes to sign in when there is no session', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const path = String(input);
      if (path === '/api/auth/me') {
        return jsonResponse({ code: 'UNAUTHENTICATED', message: 'Authentication is required' }, 401);
      }
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/tasks');

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/login');
  });

  it('signs in and returns to the originally requested protected route', async () => {
    let authenticated = false;
    let loginBody: Record<string, unknown> | null = null;
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/api/auth/me') {
        return authenticated
          ? jsonResponse({ user: authenticatedUser })
          : jsonResponse({ code: 'UNAUTHENTICATED', message: 'Authentication is required' }, 401);
      }
      if (path === '/api/csrf') return csrfResponse();
      if (path === '/api/auth/login' && init?.method === 'POST') {
        loginBody = JSON.parse(String(init.body)) as Record<string, unknown>;
        authenticated = true;
        return jsonResponse({ user: authenticatedUser });
      }
      if (path === '/api/tasks') return jsonResponse({ items: [] });
      if (path === '/api/tasks?status=COMPLETED') return jsonResponse({ items: [] });
      if (path === '/api/courses') return jsonResponse({ items: [] });
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/tasks');
    await screen.findByRole('heading', { name: 'Sign in' });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'student@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { name: 'Tasks' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/tasks');
    expect(loginBody).toEqual({ email: 'student@example.com', password: 'password123' });
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
  });

  it('registers with the detected time zone and opens Today', async () => {
    let authenticated = false;
    let registerBody: Record<string, unknown> | null = null;
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/api/auth/me') {
        return authenticated
          ? jsonResponse({ user: authenticatedUser })
          : jsonResponse({ code: 'UNAUTHENTICATED', message: 'Authentication is required' }, 401);
      }
      if (path === '/api/csrf') return csrfResponse();
      if (path === '/api/auth/register' && init?.method === 'POST') {
        registerBody = JSON.parse(String(init.body)) as Record<string, unknown>;
        authenticated = true;
        return jsonResponse({ user: authenticatedUser }, 201);
      }
      if (path === '/api/tasks') return jsonResponse({ items: [] });
      if (path === '/api/today') return jsonResponse(emptyTodayResponse());
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/register');
    await screen.findByRole('heading', { name: 'Create your account' });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'student@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('heading', { name: 'Today' })).toBeInTheDocument();
    const submittedRegisterBody = registerBody as Record<string, unknown> | null;
    expect(submittedRegisterBody).toMatchObject({ email: 'student@example.com', password: 'password123' });
    expect(typeof submittedRegisterBody?.timeZone).toBe('string');
    expect(String(submittedRegisterBody?.timeZone).length).toBeGreaterThan(0);
  });

  it('shows the generic invalid-credentials error without leaving sign in', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/api/auth/me') {
        return jsonResponse({ code: 'UNAUTHENTICATED', message: 'Authentication is required' }, 401);
      }
      if (path === '/api/csrf') return csrfResponse();
      if (path === '/api/auth/login' && init?.method === 'POST') {
        return jsonResponse({ code: 'INVALID_CREDENTIALS', message: 'Email or password is incorrect' }, 401);
      }
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/login');
    await screen.findByRole('heading', { name: 'Sign in' });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'student@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong-password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Email or password is incorrect')).toBeInTheDocument();
    expect(window.location.pathname).toBe('/login');
  });

  it('logs out and returns to sign in', async () => {
    let authenticated = true;
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/api/auth/me') {
        return authenticated
          ? jsonResponse({ user: authenticatedUser })
          : jsonResponse({ code: 'UNAUTHENTICATED', message: 'Authentication is required' }, 401);
      }
      if (path === '/api/csrf') return csrfResponse();
      if (path === '/api/auth/logout' && init?.method === 'POST') {
        authenticated = false;
        return new Response(null, { status: 204 });
      }
      if (path === '/api/tasks') return jsonResponse({ items: [] });
      if (path === '/api/today') return jsonResponse(emptyTodayResponse());
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/');
    await screen.findByRole('heading', { name: 'Today' });
    fireEvent.click(screen.getByRole('button', { name: 'Log out' }));

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    await waitFor(() => expect(window.location.pathname).toBe('/login'));
  });
});
