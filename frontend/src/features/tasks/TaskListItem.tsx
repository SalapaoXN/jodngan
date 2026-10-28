import type { Task } from './types';
import { formatRemainingTime } from './deadlineLabel';

export function TaskListItem({ task }: { task: Task }) {
  const dueDate = new Date(task.dueAt);
  const localDueDate = new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(dueDate);

  return (
    <li className="task-card">
      <article>
        <h3>{task.title}</h3>
        <p>
          Due <time dateTime={task.dueAt}>{localDueDate}</time>
        </p>
        <p className={task.planning.overdue ? 'deadline overdue' : 'deadline'}>
          {formatRemainingTime(task.planning.remainingSeconds, task.planning.overdue)}
        </p>
      </article>
    </li>
  );
}
