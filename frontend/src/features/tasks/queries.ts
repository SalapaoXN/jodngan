import { useQuery } from '@tanstack/react-query';

import { getActiveTasks } from './api';

export const activeTasksQueryKey = ['tasks', 'active'] as const;

export function useActiveTasks() {
  return useQuery({
    queryKey: activeTasksQueryKey,
    queryFn: getActiveTasks,
  });
}
