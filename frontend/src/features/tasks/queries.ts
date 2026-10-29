import { useQuery } from '@tanstack/react-query';

import { getActiveTasks } from './api';

export const activeTasksQueryKey = ['tasks', 'active'] as const;
export const activeTasksQueryKeyFor = (courseId?: string | null) => [...activeTasksQueryKey, courseId ?? 'ALL'] as const;

export function useActiveTasks(courseId?: string | null) {
  return useQuery({
    queryKey: activeTasksQueryKeyFor(courseId),
    queryFn: () => getActiveTasks(courseId),
  });
}
