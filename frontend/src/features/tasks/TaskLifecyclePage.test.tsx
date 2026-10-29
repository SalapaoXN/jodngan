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
    course: null,
    estimatedMinutes: null,
    importance: null,
    notes: null,
    status: 'ACTIVE',
    completedAt: null,
    createdAt: '2030-03-01T00:00:00Z',
    updatedAt: '2030-03-01T00:00:00Z',
    planning: {
      remainingSeconds: 7_200,
      overdue: false,
      risk: {
        state: 'INSUFFICIENT_DATA',
        reasonCode: 'EFFORT_UNKNOWN',
        message: 'เพิ่มเวลาที่คาดว่าจะใช้เพื่อประเมินความเสี่ยงได้ดีขึ้น',
      },
    },
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
      planning: { remainingSeconds: -3_600, overdue: false, risk: null },
    });
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const path = String(input);
      if (path === '/api/tasks') return jsonResponse(taskList([activeTask]));
      if (path === '/api/tasks?status=COMPLETED') return jsonResponse(taskList([completedTask]));
      if (path === '/api/courses') return jsonResponse({ items: [] });
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
          planning: { ...current.planning, overdue: false, risk: null },
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
      if (path === '/api/courses') return jsonResponse({ items: [] });
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/tasks');

    fireEvent.click(await screen.findByRole('button', { name: 'Complete' }));
    expect(await screen.findByRole('heading', { name: 'No active tasks' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: 'Completed' }));
    expect(await screen.findByRole('heading', { name: 'Database Assignment' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reopen' })).toBeInTheDocument();
  });

  it('edits task detail, changes course, and can reopen a completed task', async () => {
    const course = { id: '11111111-1111-1111-1111-111111111111', name: 'ISD' };
    let current = task({
      status: 'COMPLETED',
      completedAt: '2030-03-02T00:00:00Z',
      planning: { remainingSeconds: 7_200, overdue: false, risk: null },
    });
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === `/api/tasks/${current.id}` && init?.method === 'PUT') {
        const body = JSON.parse(String(init.body)) as {
          title: string;
          dueAt: string;
          courseId: string | null;
          estimatedMinutes: number | null;
          importance: Task['importance'];
          notes: string | null;
        };
        current = {
          ...current,
          title: body.title,
          dueAt: body.dueAt,
          course: body.courseId === course.id ? course : null,
          estimatedMinutes: body.estimatedMinutes,
          importance: body.importance,
          notes: body.notes,
        };
        return jsonResponse(current);
      }
      if (path === `/api/tasks/${current.id}/reopen` && init?.method === 'POST') {
        current = {
          ...current,
          status: 'ACTIVE',
          completedAt: null,
          planning: {
            ...current.planning,
            risk: {
              state: current.estimatedMinutes === null ? 'INSUFFICIENT_DATA' : 'NO_CONFIRMED_RISK',
              reasonCode: current.estimatedMinutes === null ? 'EFFORT_UNKNOWN' : null,
              message: current.estimatedMinutes === null
                ? 'เพิ่มเวลาที่คาดว่าจะใช้เพื่อประเมินความเสี่ยงได้ดีขึ้น'
                : null,
            },
          },
        };
        return jsonResponse(current);
      }
      if (path === `/api/tasks/${current.id}`) return jsonResponse(current);
      if (path === '/api/tasks') return jsonResponse(taskList(current.status === 'ACTIVE' ? [current] : []));
      if (path === '/api/tasks?status=COMPLETED') return jsonResponse(taskList(current.status === 'COMPLETED' ? [current] : []));
      if (path === '/api/today') {
        return jsonResponse({ generatedAt: '2030-03-01T00:00:00Z', primaryRecommendation: null, upcomingTasks: [] });
      }
      if (path === '/api/courses') return jsonResponse({ items: [course] });
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp(`/tasks/${current.id}`);

    const titleInput = await screen.findByLabelText('Task name');
    await screen.findByRole('option', { name: 'ISD' });
    fireEvent.change(titleInput, { target: { value: 'Updated Database Assignment' } });
    fireEvent.change(screen.getByRole('combobox', { name: /Course/ }), { target: { value: course.id } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('heading', { name: 'Updated Database Assignment' })).toBeInTheDocument();
    await waitFor(() => {
      const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(putCall).toBeDefined();
      expect(JSON.parse(String(putCall?.[1]?.body))).toMatchObject({
        title: 'Updated Database Assignment',
        courseId: course.id,
      });
    });

    fireEvent.click(screen.getByRole('button', { name: 'Reopen task' }));
    expect(await screen.findByText('Active', { selector: '.status-pill' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mark complete' })).toBeInTheDocument();
  });

  it('adds and clears optional planning details while showing backend risk', async () => {
    let current = task();
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === `/api/tasks/${current.id}` && init?.method === 'PUT') {
        const body = JSON.parse(String(init.body)) as {
          title: string;
          dueAt: string;
          estimatedMinutes: number | null;
          importance: Task['importance'];
          notes: string | null;
        };
        current = {
          ...current,
          ...body,
          planning: {
            ...current.planning,
            risk: body.estimatedMinutes === null
              ? {
                  state: 'INSUFFICIENT_DATA',
                  reasonCode: 'EFFORT_UNKNOWN',
                  message: 'เพิ่มเวลาที่คาดว่าจะใช้เพื่อประเมินความเสี่ยงได้ดีขึ้น',
                }
              : {
                  state: 'NO_CONFIRMED_RISK',
                  reasonCode: null,
                  message: null,
                },
          },
        };
        return jsonResponse(current);
      }
      if (path === `/api/tasks/${current.id}`) return jsonResponse(current);
      if (path === '/api/tasks') return jsonResponse(taskList([current]));
      if (path === '/api/tasks?status=COMPLETED') return jsonResponse(taskList());
      if (path === '/api/today') {
        return jsonResponse({ generatedAt: '2030-03-01T00:00:00Z', primaryRecommendation: null, upcomingTasks: [] });
      }
      if (path === '/api/courses') return jsonResponse({ items: [] });
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp(`/tasks/${current.id}`);

    expect(await screen.findByText('Add effort for a better risk check')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Estimated effort'), { target: { value: '180' } });
    fireEvent.change(screen.getByLabelText('Importance'), { target: { value: 'HIGH' } });
    fireEvent.change(screen.getByLabelText('Notes'), { target: { value: 'Split the report into sections' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByText('No confirmed risk')).toBeInTheDocument();
    await waitFor(() => {
      const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(JSON.parse(String(putCall?.[1]?.body))).toMatchObject({
        courseId: null,
        estimatedMinutes: 180,
        importance: 'HIGH',
        notes: 'Split the report into sections',
      });
    });

    fireEvent.change(screen.getByLabelText('Estimated effort'), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText('Importance'), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText('Notes'), { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByText('Add effort for a better risk check')).toBeInTheDocument();
    await waitFor(() => {
      const putCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === 'PUT');
      const lastPutCall = putCalls[putCalls.length - 1];
      expect(JSON.parse(String(lastPutCall?.[1]?.body))).toMatchObject({
        courseId: null,
        estimatedMinutes: null,
        importance: null,
        notes: null,
      });
    });
  });
});
