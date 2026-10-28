import { useQuery } from '@tanstack/react-query';

import { getToday } from './api';

export const todayQueryKey = ['today'] as const;

export function useToday() {
  return useQuery({
    queryKey: todayQueryKey,
    queryFn: getToday,
  });
}
