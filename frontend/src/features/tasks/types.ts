import type { TaskImportance, TaskRisk } from './planningTypes';

export interface TaskPlanning {
  remainingSeconds: number;
  overdue: boolean;
  risk: TaskRisk | null;
}

export interface Task {
  id: string;
  title: string;
  dueAt: string;
  estimatedMinutes: number | null;
  importance: TaskImportance | null;
  notes: string | null;
  status: 'ACTIVE' | 'COMPLETED';
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  planning: TaskPlanning;
}

export interface TaskListResponse {
  items: Task[];
}

export interface CreateTaskInput {
  title: string;
  dueAt: string;
}

export interface UpdateTaskInput {
  title: string;
  dueAt: string;
  estimatedMinutes: number | null;
  importance: TaskImportance | null;
  notes: string | null;
}
