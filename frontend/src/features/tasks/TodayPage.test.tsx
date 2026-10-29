import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from '../../app/App';
import { AppProviders } from '../../app/providers/AppProviders';
import type { TodayRecommendation, TodayResponse } from '../today/types';
import type { Task, TaskListResponse } from './types';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function renderApp() {
  return render(
    <AppProviders>
      <App />
    </AppProviders>,
  );
}

function localTodayAt(time: string): string {
  const today = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T${time}`;
}

function chooseTodayAt(time: string) {
  const [hour, minute] = time.split(':');
  fireEvent.click(screen.getByLabelText('Due date and time'));
  const picker = screen.getByRole('group', { name: 'Due date and time picker' });
  fireEvent.click(within(picker).getByRole('button', { name: 'Today' }));
  fireEvent.change(within(picker).getByLabelText('Hour'), { target: { value: hour } });
  fireEvent.change(within(picker).getByLabelText('Minute'), { target: { value: minute } });
  fireEvent.click(within(picker).getByRole('button', { name: 'Done' }));
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

function recommendation(
  recommendedTask: Task,
  overrides: Partial<TodayRecommendation> = {},
): TodayRecommendation {
  return {
    task: recommendedTask,
    urgency: 'HIGH',
    reasonCode: 'DUE_SOON',
    reason: 'กำหนดส่งใกล้เข้ามา',
    startGuidance: 'START_TODAY',
    ...overrides,
  };
}

function todayResponse(
  primaryRecommendation: TodayRecommendation | null = null,
  upcomingTasks: TodayRecommendation[] = [],
): TodayResponse {
  return {
    generatedAt: '2030-03-01T00:00:00Z',
    primaryRecommendation,
    upcomingTasks,
  };
}

function mockReadApis(items: Task[] = [], today = todayResponse()) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const path = String(input);
    if (path === '/api/tasks' && init?.method !== 'POST') {
      return jsonResponse(taskList(items));
    }
    if (path === '/api/today') {
      return jsonResponse(today);
    }
    throw new Error(`Unexpected request: ${path}`);
  });
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe('single-user Today flow', () => {
  it('opens directly on Today and shows the empty states', async () => {
    mockReadApis();

    renderApp();

    expect(screen.getByRole('heading', { name: 'Today' })).toBeInTheDocument();
    expect(await screen.findByText(/เพิ่มงานเพื่อให้ JodNgan/)).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'No tasks yet' })).toBeInTheDocument();
  });

  it('shows independent loading states for recommendation and task list', () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(() => new Promise<Response>(() => undefined));

    renderApp();

    expect(screen.getByText('กำลังจัดลำดับงาน…')).toBeInTheDocument();
    expect(screen.getByText('Loading tasks…')).toBeInTheDocument();
  });

  it('opens Quick Add and reports the two required fields', async () => {
    mockReadApis();
    renderApp();

    fireEvent.click(await screen.findByRole('button', { name: 'Add task' }));
    fireEvent.click(screen.getByRole('button', { name: 'Save task' }));

    expect(await screen.findByText('Enter a task name.')).toBeInTheDocument();
    expect(screen.getByText('Choose a due date and time.')).toBeInTheDocument();
  });

  it('saves a minimal task and refreshes the recommendation', async () => {
    const dueAtInput = localTodayAt('13:45');
    const savedTask = task({
      dueAt: new Date(dueAtInput).toISOString(),
      planning: { remainingSeconds: 7_200, overdue: false },
    });
    let saved = false;
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/api/tasks' && init?.method === 'POST') {
        saved = true;
        return jsonResponse(savedTask, 201);
      }
      if (path === '/api/tasks') {
        return jsonResponse(taskList(saved ? [savedTask] : []));
      }
      if (path === '/api/today') {
        return jsonResponse(todayResponse(saved ? recommendation(savedTask) : null));
      }
      throw new Error(`Unexpected request: ${path}`);
    });
    renderApp();

    fireEvent.click(await screen.findByRole('button', { name: 'Add task' }));
    fireEvent.change(screen.getByLabelText('Task name'), {
      target: { value: '  Database Assignment  ' },
    });
    chooseTodayAt('13:45');
    fireEvent.click(screen.getByRole('button', { name: 'Save task' }));

    expect((await screen.findAllByRole('heading', { name: 'Database Assignment' })).length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: 'Open →' })).toHaveAttribute(
      'href',
      `/tasks/${savedTask.id}`,
    );
    expect(await screen.findByText('กำหนดส่งใกล้เข้ามา')).toBeInTheDocument();
    expect(screen.getByText('เริ่มวันนี้')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Quick Add' })).not.toBeInTheDocument();

    await waitFor(() => {
      const postCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      expect(postCall).toBeDefined();
      expect(JSON.parse(postCall?.[1]?.body as string)).toEqual({
        title: 'Database Assignment',
        dueAt: new Date(dueAtInput).toISOString(),
      });
      expect(fetchMock.mock.calls.filter(([input]) => String(input) === '/api/today').length).toBeGreaterThan(1);
    });
  });

  it('renders overdue feedback and start-now guidance from the Today API', async () => {
    const overdueTask = task({ planning: { remainingSeconds: -10_800, overdue: true } });
    mockReadApis(
      [overdueTask],
      todayResponse(recommendation(overdueTask, {
        urgency: 'OVERDUE',
        reasonCode: 'OVERDUE',
        reason: 'เลยกำหนดส่งแล้ว',
        startGuidance: 'START_NOW',
      })),
    );
    renderApp();

    expect(await screen.findByText('เลยกำหนดส่งแล้ว')).toBeInTheDocument();
    expect(screen.getByText('เริ่มตอนนี้')).toBeInTheDocument();
    expect(screen.getAllByText('เลยกำหนดแล้ว 3 ชั่วโมง').length).toBeGreaterThan(0);
  });

  it('uses the backend-selected primary recommendation without reranking in React', async () => {
    const closerTask = task({
      id: '00000000-0000-0000-0000-000000000001',
      title: 'Closer in task list',
      dueAt: '2030-03-02T00:00:00Z',
    });
    const backendChoice = task({
      id: '00000000-0000-0000-0000-000000000002',
      title: 'Backend choice',
      dueAt: '2030-03-10T00:00:00Z',
    });
    mockReadApis(
      [closerTask, backendChoice],
      todayResponse(recommendation(backendChoice, { reason: 'เป็นงานถัดไปตามกำหนดส่ง', startGuidance: 'CAN_WAIT' })),
    );
    renderApp();

    const recommendationSection = screen.getByRole('region', { name: 'Today recommendation' });
    expect(await within(recommendationSection).findByRole('heading', { name: 'Backend choice' })).toBeInTheDocument();
  });

  it('shows a task-list error and recovers without blocking recommendation', async () => {
    let taskAttempts = 0;
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const path = String(input);
      if (path === '/api/today') {
        return jsonResponse(todayResponse());
      }
      if (path === '/api/tasks') {
        taskAttempts += 1;
        return taskAttempts === 1
          ? jsonResponse({ code: 'INTERNAL_ERROR', message: 'Tasks unavailable' }, 500)
          : jsonResponse(taskList());
      }
      throw new Error(`Unexpected request: ${path}`);
    });
    renderApp();

    expect(await screen.findByText('Tasks unavailable')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('heading', { name: 'No tasks yet' })).toBeInTheDocument();
  });

  it('shows a recommendation error and recovers without blocking Quick Add', async () => {
    const selectedTask = task();
    let todayAttempts = 0;
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const path = String(input);
      if (path === '/api/tasks') {
        return jsonResponse(taskList([selectedTask]));
      }
      if (path === '/api/today') {
        todayAttempts += 1;
        return todayAttempts === 1
          ? jsonResponse({ code: 'INTERNAL_ERROR', message: 'Recommendation unavailable' }, 500)
          : jsonResponse(todayResponse(recommendation(selectedTask)));
      }
      throw new Error(`Unexpected request: ${path}`);
    });
    renderApp();

    expect(await screen.findByText('Recommendation unavailable')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add task' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Try recommendation again' }));
    expect(await screen.findByText('กำหนดส่งใกล้เข้ามา')).toBeInTheDocument();
  });

  it('keeps Quick Add open after a save error so the user can retry', async () => {
    const savedTask = task({ title: 'Retry task' });
    let postAttempts = 0;
    let saved = false;
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/api/tasks' && init?.method === 'POST') {
        postAttempts += 1;
        if (postAttempts === 1) {
          return jsonResponse({ code: 'INTERNAL_ERROR', message: 'Task could not be saved' }, 500);
        }
        saved = true;
        return jsonResponse(savedTask, 201);
      }
      if (path === '/api/tasks') {
        return jsonResponse(taskList(saved ? [savedTask] : []));
      }
      if (path === '/api/today') {
        return jsonResponse(todayResponse(saved ? recommendation(savedTask) : null));
      }
      throw new Error(`Unexpected request: ${path}`);
    });
    renderApp();

    fireEvent.click(await screen.findByRole('button', { name: 'Add task' }));
    fireEvent.change(screen.getByLabelText('Task name'), { target: { value: 'Retry task' } });
    chooseTodayAt('13:45');
    fireEvent.click(screen.getByRole('button', { name: 'Save task' }));

    expect(await screen.findByText(/Task could not be saved/)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Quick Add' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Save task' }));
    expect((await screen.findAllByRole('heading', { name: 'Retry task' })).length).toBeGreaterThan(0);
  });
});
