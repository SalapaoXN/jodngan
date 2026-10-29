import type { TaskRisk } from './planningTypes';

const riskCopy = {
  AT_RISK: {
    label: 'At risk',
    title: 'This task needs attention',
    fallback: 'The current deadline and effort estimate indicate a confirmed risk.',
  },
  NO_CONFIRMED_RISK: {
    label: 'Risk checked',
    title: 'No confirmed risk',
    fallback: 'The estimated effort still fits within the clock time before the deadline.',
  },
  INSUFFICIENT_DATA: {
    label: 'Limited estimate',
    title: 'Add effort for a better risk check',
    fallback: 'Add estimated effort to let JodNgan evaluate workload risk more reliably.',
  },
} as const;

export function RiskInsight({ risk, compact = false }: { risk: TaskRisk; compact?: boolean }) {
  const copy = riskCopy[risk.state];

  return (
    <div className={`risk-insight risk-${risk.state.toLowerCase().replace(/_/g, '-')} ${compact ? 'compact' : ''}`}>
      <div className="risk-insight-icon" aria-hidden="true">
        {risk.state === 'AT_RISK' ? '!' : risk.state === 'NO_CONFIRMED_RISK' ? '✓' : '?'}
      </div>
      <div>
        <p className="risk-insight-label">{copy.label}</p>
        {!compact && <strong>{copy.title}</strong>}
        {!compact && <p>{risk.message ?? copy.fallback}</p>}
      </div>
    </div>
  );
}
