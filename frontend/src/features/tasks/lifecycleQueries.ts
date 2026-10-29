import { useQuery } from '@tanstack/react-query';

import { getCompletedTasks, getTask } from './api';

export const completedTasksQueryKey = ['tasks', 'completed'] as const;
export const taskDetailQueryKey = (taskId: string) => ['tasks', 'detail', taskId] as const;

export function useCompletedTasks() {
  return useQuery({
    queryKey: completedTasksQueryKey,
    queryFn: getCompletedTasks,
  });
}

export function useTaskDetail(taskId: string) {
  return useQuery({
    queryKey: taskDetailQueryKey(taskId),
    queryFn: () => getTask(taskId),
    enabled: taskId.length > 0,
  });
}
