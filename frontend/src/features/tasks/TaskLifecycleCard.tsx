import { Link } from 'react-router';

import { formatRemainingTime } from './deadlineLabel';
import type { Task } from './types';

interface TaskLifecycleCardProps {
  task: Task;
  actionLabel: string;
  actionPending: boolean;
  onAction: () => void;
}

export function TaskLifecycleCard({ task, actionLabel, actionPending, onAction }: TaskLifecycleCardProps) {
  const dueDate = new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(task.dueAt));
  const completedDate = task.completedAt
    ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(task.completedAt))
    : null;

  return (
    <li className="task-card lifecycle-card">
      <article>
        <div className="task-card-heading">
          <div className="task-card-main">
            <div className="task-card-title-row">
              <span className={task.status === 'ACTIVE' ? 'task-state-dot active' : 'task-state-dot completed'} aria-hidden="true" />
              <h3><Link to={`/tasks/${task.id}`}>{task.title}</Link></h3>
            </div>
            <p className="task-due-line">Due <time dateTime={task.dueAt}>{dueDate}</time></p>
          </div>
          <button type="button" className="secondary-button lifecycle-action" disabled={actionPending} onClick={onAction}>
            {actionPending ? 'Saving…' : actionLabel}
          </button>
        </div>

        <div className="task-card-status-row">
          {task.status === 'COMPLETED' ? (
            <p className="completed-label">
              Completed{completedDate && task.completedAt ? <> <time dateTime={task.completedAt}>{completedDate}</time></> : null}
            </p>
          ) : (
            <p className={task.planning.overdue ? 'deadline overdue' : 'deadline'}>
              {formatRemainingTime(task.planning.remainingSeconds, task.planning.overdue)}
            </p>
          )}
          <Link className="task-open-link" to={`/tasks/${task.id}`}>Open details →</Link>
        </div>
      </article>
    </li>
  );
}
