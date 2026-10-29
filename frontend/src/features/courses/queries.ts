import { useQuery } from '@tanstack/react-query';

import { getCourses } from './api';

export const coursesQueryKey = ['courses'] as const;

export function useCourses() {
  return useQuery({
    queryKey: coursesQueryKey,
    queryFn: getCourses,
  });
}
