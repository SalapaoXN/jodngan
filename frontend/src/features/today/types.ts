import type { Task } from '../tasks/types';

export type UrgencyBand = 'OVERDUE' | 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type RecommendationReason =
  | 'OVERDUE'
  | 'DUE_WITHIN_24_HOURS'
  | 'DUE_SOON'
  | 'UPCOMING';
export type StartGuidance = 'START_NOW' | 'START_TODAY' | 'START_SOON' | 'CAN_WAIT';

export interface TodayRecommendation {
  task: Task;
  urgency: UrgencyBand;
  reasonCode: RecommendationReason;
  reason: string;
  startGuidance: StartGuidance;
}

export interface TodayResponse {
  generatedAt: string;
  primaryRecommendation: TodayRecommendation | null;
  upcomingTasks: TodayRecommendation[];
}
