export type TaskImportance = 'LOW' | 'NORMAL' | 'HIGH';
export type RiskState = 'AT_RISK' | 'NO_CONFIRMED_RISK' | 'INSUFFICIENT_DATA';
export type RiskReason = 'OVERDUE' | 'EFFORT_UNKNOWN' | 'INSUFFICIENT_CLOCK_TIME';

export interface TaskRisk {
  state: RiskState;
  reasonCode: RiskReason | null;
  message: string | null;
}
