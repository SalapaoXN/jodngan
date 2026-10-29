import { Link } from 'react-router';

import type { Task } from './types';
import { formatRemainingTime } from './deadlineLabel';

export function TaskListItem({ task }: { task: Task }) {
  const dueDate = new Date(task.dueAt);
  const localDueDate = new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(dueDate);

  return (
    <li className="task-card compact-task-card">
      <article>
        <div className="task-card-title-row">
          <span className={task.planning.overdue ? 'task-state-dot overdue' : 'task-state-dot active'} aria-hidden="true" />
          <h3><Link to={`/tasks/${task.id}`}>{task.title}</Link></h3>
        </div>
        <div className="task-card-status-row">
          <div>
            <p className="task-due-line">
              Due <time dateTime={task.dueAt}>{localDueDate}</time>
            </p>
            <p className={task.planning.overdue ? 'deadline overdue' : 'deadline'}>
              {formatRemainingTime(task.planning.remainingSeconds, task.planning.overdue)}
            </p>
          </div>
          <Link className="task-open-link" to={`/tasks/${task.id}`}>Open →</Link>
        </div>
      </article>
    </li>
  );
}
