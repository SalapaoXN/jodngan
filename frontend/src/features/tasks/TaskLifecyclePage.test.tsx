import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from '../../app/App';
import { AppProviders } from '../../app/providers/AppProviders';
import type { Task, TaskListResponse } from './types';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function renderApp(path: string) {
  window.history.pushState({}, '', path);
  return render(
    <AppProviders>
      <App />
    </AppProviders>,
  );
}

function task(overrides: Partial<Task> = {}): Task {
  return {
    id: 'a6c0ce45-9efb-4c26-bcf4-4da02cbcc4fb',
    title: 'Database Assignment',
    dueAt: '2030-03-15T13:45:00Z',
    status: 'ACTIVE',
    completedAt: null,
    createdAt: '2030-03-01T00:00:00Z',
    updatedAt: '2030-03-01T00:00:00Z',
    planning: { remainingSeconds: 7_200, overdue: false },
    ...overrides,
  };
}

function taskList(items: Task[] = []): TaskListResponse {
  return { items };
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  window.history.pushState({}, '', '/');
});

describe('Task lifecycle flow', () => {
  it('switches between active and completed task lists', async () => {
    const activeTask = task();
    const completedTask = task({
      id: 'b6c0ce45-9efb-4c26-bcf4-4da02cbcc4fb',
      title: 'Finished report',
      status: 'COMPLETED',
      completedAt: '2030-03-02T00:00:00Z',
      planning: { remainingSeconds: -3_600, overdue: false },
    });
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const path = String(input);
      if (path === '/api/tasks') return jsonResponse(taskList([activeTask]));
      if (path === '/api/tasks?status=COMPLETED') return jsonResponse(taskList([completedTask]));
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/tasks');

    expect(await screen.findByRole('heading', { name: 'Database Assignment' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Completed' }));
    expect(await screen.findByRole('heading', { name: 'Finished report' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open details →' })).toHaveAttribute(
      'href',
      `/tasks/${completedTask.id}`,
    );
    expect(screen.getAllByText(/Completed/).length).toBeGreaterThan(1);
  });

  it('completes an active task and moves it to the completed tab', async () => {
    let current = task();
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === `/api/tasks/${current.id}/complete` && init?.method === 'POST') {
        current = {
          ...current,
          status: 'COMPLETED',
          completedAt: '2030-03-02T00:00:00Z',
          planning: { ...current.planning, overdue: false },
        };
        return jsonResponse(current);
      }
      if (path === '/api/tasks') {
        return jsonResponse(taskList(current.status === 'ACTIVE' ? [current] : []));
      }
      if (path === '/api/tasks?status=COMPLETED') {
        return jsonResponse(taskList(current.status === 'COMPLETED' ? [current] : []));
      }
      if (path === '/api/today') {
        return jsonResponse({ generatedAt: '2030-03-01T00:00:00Z', primaryRecommendation: null, upcomingTasks: [] });
      }
      if (path === `/api/tasks/${current.id}`) return jsonResponse(current);
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/tasks');

    fireEvent.click(await screen.findByRole('button', { name: 'Complete' }));
    expect(await screen.findByRole('heading', { name: 'No active tasks' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: 'Completed' }));
    expect(await screen.findByRole('heading', { name: 'Database Assignment' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reopen' })).toBeInTheDocument();
  });

  it('edits task detail and can reopen a completed task', async () => {
    let current = task({
      status: 'COMPLETED',
      completedAt: '2030-03-02T00:00:00Z',
      planning: { remainingSeconds: 7_200, overdue: false },
    });
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === `/api/tasks/${current.id}` && init?.method === 'PUT') {
        const body = JSON.parse(String(init.body)) as { title: string; dueAt: string };
        current = { ...current, title: body.title, dueAt: body.dueAt };
        return jsonResponse(current);
      }
      if (path === `/api/tasks/${current.id}/reopen` && init?.method === 'POST') {
        current = { ...current, status: 'ACTIVE', completedAt: null };
        return jsonResponse(current);
      }
      if (path === `/api/tasks/${current.id}`) return jsonResponse(current);
      if (path === '/api/tasks') return jsonResponse(taskList(current.status === 'ACTIVE' ? [current] : []));
      if (path === '/api/tasks?status=COMPLETED') return jsonResponse(taskList(current.status === 'COMPLETED' ? [current] : []));
      if (path === '/api/today') {
        return jsonResponse({ generatedAt: '2030-03-01T00:00:00Z', primaryRecommendation: null, upcomingTasks: [] });
      }
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp(`/tasks/${current.id}`);

    const titleInput = await screen.findByLabelText('Task name');
    fireEvent.change(titleInput, { target: { value: 'Updated Database Assignment' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('heading', { name: 'Updated Database Assignment' })).toBeInTheDocument();
    await waitFor(() => {
      const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(putCall).toBeDefined();
      expect(JSON.parse(String(putCall?.[1]?.body))).toMatchObject({ title: 'Updated Database Assignment' });
    });

    fireEvent.click(screen.getByRole('button', { name: 'Reopen task' }));
    expect(await screen.findByText('Active', { selector: '.status-pill' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mark complete' })).toBeInTheDocument();
  });
});
