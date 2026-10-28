import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from './App';
import { AppProviders } from './providers/AppProviders';

describe('App foundation', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the backend health status', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ status: 'ok' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(screen.getByRole('heading', { name: /foundation ready/i })).toBeInTheDocument();
    expect(await screen.findByText(/API status: ok/i)).toBeInTheDocument();
  });
});
