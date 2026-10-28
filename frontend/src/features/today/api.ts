import { apiFetch } from '../../lib/api/client';
import type { TodayResponse } from './types';

export function getToday(): Promise<TodayResponse> {
  return apiFetch<TodayResponse>('/api/today');
}
