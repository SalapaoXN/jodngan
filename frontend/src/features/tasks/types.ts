export interface TaskPlanning {
  remainingSeconds: number;
  overdue: boolean;
}

export interface Task {
  id: string;
  title: string;
  dueAt: string;
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
}
