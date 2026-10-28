import { apiFetch } from '../../lib/api/client';
import type { CreateTaskInput, Task, TaskListResponse } from './types';

export function getActiveTasks(): Promise<TaskListResponse> {
  return apiFetch<TaskListResponse>('/api/tasks');
}

export function createTask(input: CreateTaskInput): Promise<Task> {
  return apiFetch<Task>('/api/tasks', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}
