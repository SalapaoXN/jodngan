import { useQuery } from '@tanstack/react-query';

import { ApiError } from '../../lib/api/client';
import { getCurrentUser } from './api';
import type { User } from './types';

export const currentUserQueryKey = ['auth', 'current-user'] as const;

async function currentUserOrNull(): Promise<User | null> {
  try {
    return await getCurrentUser();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}

export function useCurrentUser() {
  return useQuery({
    queryKey: currentUserQueryKey,
    queryFn: currentUserOrNull,
    retry: false,
  });
}
