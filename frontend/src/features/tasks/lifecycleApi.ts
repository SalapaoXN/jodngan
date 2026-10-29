import { apiFetch } from '../../lib/api/client';
import type { Task, UpdateTaskInput } from './types';

export function updateTask(taskId: string, input: UpdateTaskInput): Promise<Task> {
  return apiFetch<Task>(`/api/tasks/${taskId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function completeTask(taskId: string): Promise<Task> {
  return apiFetch<Task>(`/api/tasks/${taskId}/complete`, { method: 'POST' });
}

export function reopenTask(taskId: string): Promise<Task> {
  return apiFetch<Task>(`/api/tasks/${taskId}/reopen`, { method: 'POST' });
}
