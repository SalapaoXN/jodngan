import { apiFetch } from '../../lib/api/client';
import type { CreateTaskInput, Task, TaskListResponse } from './types';

export function getActiveTasks(courseId?: string | null): Promise<TaskListResponse> {
  const path = courseId ? `/api/tasks?courseId=${encodeURIComponent(courseId)}` : '/api/tasks';
  return apiFetch<TaskListResponse>(path);
}

export function getCompletedTasks(courseId?: string | null): Promise<TaskListResponse> {
  const path = courseId
    ? `/api/tasks?status=COMPLETED&courseId=${encodeURIComponent(courseId)}`
    : '/api/tasks?status=COMPLETED';
  return apiFetch<TaskListResponse>(path);
}

export function getTask(taskId: string): Promise<Task> {
  return apiFetch<Task>(`/api/tasks/${taskId}`);
}

export function createTask(input: CreateTaskInput): Promise<Task> {
  return apiFetch<Task>('/api/tasks', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}
