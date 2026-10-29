import { apiFetch } from '../../lib/api/client';
import { refreshCsrfToken } from '../../lib/api/csrf';
import type { AuthResponse, LoginInput, RegisterInput, User } from './types';

export async function getCurrentUser(): Promise<User> {
  return (await apiFetch<AuthResponse>('/api/auth/me')).user;
}

export async function login(input: LoginInput): Promise<User> {
  const response = await apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  });
  await refreshCsrfToken();
  return response.user;
}

export async function register(input: RegisterInput): Promise<User> {
  const response = await apiFetch<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  });
  await refreshCsrfToken();
  return response.user;
}

export async function logout(): Promise<void> {
  await apiFetch<void>('/api/auth/logout', { method: 'POST' });
  await refreshCsrfToken();
}
