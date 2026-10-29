import { useQuery } from '@tanstack/react-query';

import { getCompletedTasks, getTask } from './api';

export const completedTasksQueryKey = ['tasks', 'completed'] as const;
export const completedTasksQueryKeyFor = (courseId?: string | null) => [...completedTasksQueryKey, courseId ?? 'ALL'] as const;
export const taskDetailQueryKey = (taskId: string) => ['tasks', 'detail', taskId] as const;

export function useCompletedTasks(courseId?: string | null) {
  return useQuery({
    queryKey: completedTasksQueryKeyFor(courseId),
    queryFn: () => getCompletedTasks(courseId),
  });
}

export function useTaskDetail(taskId: string) {
  return useQuery({
    queryKey: taskDetailQueryKey(taskId),
    queryFn: () => getTask(taskId),
    enabled: taskId.length > 0,
  });
}
