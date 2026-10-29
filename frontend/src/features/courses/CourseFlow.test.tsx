import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from '../../app/App';
import { AppProviders } from '../../app/providers/AppProviders';
import { mockAuthenticatedApi } from '../../test/apiTestSupport';
import type { Task } from '../tasks/types';
import type { Course } from './types';

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

function task(title: string, course: Course | null): Task {
  return {
    id: `${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}-id`,
    title,
    dueAt: '2030-03-15T13:45:00Z',
    course,
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
  };
}

function chooseAnyDateAndTime() {
  fireEvent.click(screen.getByLabelText('Due date and time'));
  const picker = screen.getByRole('group', { name: 'Due date and time picker' });
  fireEvent.click(within(picker).getByRole('button', { name: 'Today' }));
  fireEvent.change(within(picker).getByLabelText('Hour'), { target: { value: '18' } });
  fireEvent.change(within(picker).getByLabelText('Minute'), { target: { value: '30' } });
  fireEvent.click(within(picker).getByRole('button', { name: 'Done' }));
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  window.history.pushState({}, '', '/');
});

describe('Course organization', () => {
  it('keeps Quick Add optional but sends the selected course', async () => {
    const course: Course = { id: '11111111-1111-1111-1111-111111111111', name: 'ISD' };
    const savedTask = task('Course task', course);
    let saved = false;
    let postedBody: Record<string, unknown> | null = null;

    mockAuthenticatedApi(async (input, init) => {
      const path = String(input);
      if (path === '/api/courses') return jsonResponse({ items: [course] });
      if (path === '/api/tasks' && init?.method === 'POST') {
        postedBody = JSON.parse(String(init.body)) as Record<string, unknown>;
        saved = true;
        return jsonResponse(savedTask, 201);
      }
      if (path === '/api/tasks') return jsonResponse({ items: saved ? [savedTask] : [] });
      if (path === '/api/today') {
        return jsonResponse({ generatedAt: '2030-03-01T00:00:00Z', primaryRecommendation: null, upcomingTasks: [] });
      }
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/');
    fireEvent.click(await screen.findByRole('button', { name: 'Add task' }));
    fireEvent.change(screen.getByLabelText('Task name'), { target: { value: 'Course task' } });
    chooseAnyDateAndTime();
    await screen.findByRole('option', { name: 'ISD' });
    fireEvent.change(screen.getByRole('combobox', { name: /Course/ }), { target: { value: course.id } });
    fireEvent.click(screen.getByRole('button', { name: 'Save task' }));

    await screen.findByRole('heading', { name: 'Course task' });
    expect(postedBody).toMatchObject({ title: 'Course task', courseId: course.id });
    expect(screen.getByText('ISD')).toBeInTheDocument();
  });

  it('filters tasks by course and manages course lifecycle inline', async () => {
    let courses: Course[] = [
      { id: '11111111-1111-1111-1111-111111111111', name: 'ISD' },
      { id: '22222222-2222-2222-2222-222222222222', name: 'Data Warehouse' },
    ];
    const isdCourseId = courses[0].id;
    const isdTask = task('ISD Assignment', courses[0]);
    const dwTask = task('DW Assignment', courses[1]);

    mockAuthenticatedApi(async (input, init) => {
      const path = String(input);
      if (path === '/api/courses' && init?.method === 'POST') {
        const body = JSON.parse(String(init.body)) as { name: string };
        const created = { id: '33333333-3333-3333-3333-333333333333', name: body.name };
        courses = [...courses, created];
        return jsonResponse(created, 201);
      }
      if (path.startsWith('/api/courses/') && init?.method === 'PUT') {
        const id = path.split('/').at(-1) ?? '';
        const body = JSON.parse(String(init.body)) as { name: string };
        courses = courses.map((course) => course.id === id ? { ...course, name: body.name } : course);
        return jsonResponse(courses.find((course) => course.id === id));
      }
      if (path.startsWith('/api/courses/') && init?.method === 'DELETE') {
        const id = path.split('/').at(-1) ?? '';
        courses = courses.filter((course) => course.id !== id);
        return new Response(null, { status: 204 });
      }
      if (path === '/api/courses') return jsonResponse({ items: courses });
      if (path === '/api/tasks') return jsonResponse({ items: [isdTask, dwTask] });
      if (path === `/api/tasks?courseId=${encodeURIComponent(isdCourseId)}`) {
        return jsonResponse({ items: courses.some((course) => course.id === isdCourseId) ? [isdTask] : [] });
      }
      if (path === '/api/tasks?status=COMPLETED') return jsonResponse({ items: [] });
      if (path.startsWith('/api/tasks?status=COMPLETED&courseId=')) return jsonResponse({ items: [] });
      if (path === '/api/today') {
        return jsonResponse({ generatedAt: '2030-03-01T00:00:00Z', primaryRecommendation: null, upcomingTasks: [] });
      }
      throw new Error(`Unexpected request: ${path}`);
    });

    renderApp('/tasks');
    expect(await screen.findByRole('heading', { name: 'ISD Assignment' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'DW Assignment' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Course'), { target: { value: isdCourseId } });
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'DW Assignment' })).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Manage courses' }));
    const manager = screen.getByRole('region', { name: 'Course manager' });
    fireEvent.change(within(manager).getByLabelText('New course'), { target: { value: 'Cloud Computing' } });
    fireEvent.click(within(manager).getByRole('button', { name: 'Add course' }));
    expect(await within(manager).findByText('Cloud Computing')).toBeInTheDocument();

    const isdRow = within(manager).getByText('ISD').closest('li');
    expect(isdRow).not.toBeNull();
    fireEvent.click(within(isdRow as HTMLElement).getByRole('button', { name: 'Rename' }));
    fireEvent.change(within(manager).getByLabelText('Rename ISD'), { target: { value: 'Information Systems' } });
    fireEvent.click(within(isdRow as HTMLElement).getByRole('button', { name: 'Save' }));
    expect(await within(manager).findByText('Information Systems')).toBeInTheDocument();

    const renamedRow = within(manager).getByText('Information Systems').closest('li');
    expect(renamedRow).not.toBeNull();
    fireEvent.click(within(renamedRow as HTMLElement).getByRole('button', { name: 'Remove' }));
    fireEvent.click(within(renamedRow as HTMLElement).getByRole('button', { name: 'Confirm remove' }));
    await waitFor(() => expect(within(manager).queryByText('Information Systems')).not.toBeInTheDocument());
  });
});
