export interface TaskPlanning {
  remainingSeconds: number;
  overdue: boolean;
}

export interface Task {
  id: string;
  title: string;
  dueAt: string;
  status: 'ACTIVE' | 'COMPLETED';
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
