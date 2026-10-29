import { formatRemainingTime } from '../tasks/deadlineLabel';
import { RiskInsight } from '../tasks/RiskInsight';
import type { StartGuidance, TodayRecommendation } from './types';

const startGuidanceLabels: Record<StartGuidance, string> = {
  START_NOW: 'เริ่มตอนนี้',
  START_TODAY: 'เริ่มวันนี้',
  START_SOON: 'เริ่มเร็ว ๆ นี้',
  CAN_WAIT: 'ยังรอได้',
};

export function RecommendationCard({ recommendation }: { recommendation: TodayRecommendation }) {
  const dueDate = new Date(recommendation.task.dueAt);
  const localDueDate = new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(dueDate);

  return (
    <section className="recommendation-card" aria-labelledby="recommendation-heading">
      <div className="recommendation-accent" aria-hidden="true">→</div>
      <div className="recommendation-content">
        <div className="recommendation-title-row">
          <div>
            <p className="recommendation-kicker">ควรทำต่อ</p>
            <h2 id="recommendation-heading">{recommendation.task.title}</h2>
          </div>
          <span className="guidance-pill">{startGuidanceLabels[recommendation.startGuidance]}</span>
        </div>

        {recommendation.task.course && (
          <span className="course-chip recommendation-course">{recommendation.task.course.name}</span>
        )}
        <p className="recommendation-reason">{recommendation.reason}</p>

        {recommendation.task.planning.risk?.state === 'AT_RISK' && (
          <RiskInsight risk={recommendation.task.planning.risk} compact />
        )}

        <div className="recommendation-footer">
          <span className={recommendation.task.planning.overdue ? 'deadline overdue' : 'deadline'}>
            {formatRemainingTime(
              recommendation.task.planning.remainingSeconds,
              recommendation.task.planning.overdue,
            )}
          </span>
          <p className="recommendation-due">
            Due <time dateTime={recommendation.task.dueAt}>{localDueDate}</time>
          </p>
        </div>
      </div>
    </section>
  );
}
