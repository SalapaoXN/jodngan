import { ApiError } from '../../lib/api/client';
import { RecommendationCard } from '../today/RecommendationCard';
import { useToday } from '../today/queries';
import { QuickAddTask } from './QuickAddTask';
import { TaskListItem } from './TaskListItem';
import { useActiveTasks } from './queries';

export function TodayPage() {
  const todayQuery = useToday();
  const tasksQuery = useActiveTasks();

  return (
    <main className="page-shell">
      <header className="today-header">
        <p className="eyebrow">JodNgan</p>
        <h1>Today</h1>
        <p>Keep your deadlines in view and capture new work quickly.</p>
      </header>

      <section className="recommendation-section" aria-label="Today recommendation">
        {todayQuery.isPending && (
          <div className="recommendation-state">
            <p role="status">กำลังจัดลำดับงาน…</p>
          </div>
        )}

        {todayQuery.isError && (
          <div className="recommendation-state error-state" role="alert">
            <p>
              {todayQuery.error instanceof ApiError
                ? todayQuery.error.message
                : 'Recommendation could not be loaded.'}
            </p>
            <button type="button" className="secondary-button" onClick={() => void todayQuery.refetch()}>
              Try recommendation again
            </button>
          </div>
        )}

        {!todayQuery.isPending && !todayQuery.isError && todayQuery.data?.primaryRecommendation && (
          <RecommendationCard recommendation={todayQuery.data.primaryRecommendation} />
        )}

        {!todayQuery.isPending && !todayQuery.isError && !todayQuery.data?.primaryRecommendation && (
          <div className="recommendation-state recommendation-empty">
            <p>เพิ่มงานเพื่อให้ JodNgan ช่วยบอกว่างานไหนควรทำต่อ</p>
          </div>
        )}
      </section>

      <QuickAddTask />

      <section className="task-section" aria-labelledby="tasks-heading">
        <div className="section-heading">
          <h2 id="tasks-heading">Your tasks</h2>
          {tasksQuery.data && <span>{tasksQuery.data.items.length}</span>}
        </div>

        {tasksQuery.isPending && <p role="status">Loading tasks…</p>}

        {tasksQuery.isError && (
          <div className="error-state" role="alert">
            <p>
              {tasksQuery.error instanceof ApiError
                ? tasksQuery.error.message
                : 'Tasks could not be loaded.'}
            </p>
            <button type="button" className="secondary-button" onClick={() => void tasksQuery.refetch()}>
              Try again
            </button>
          </div>
        )}

        {!tasksQuery.isPending && !tasksQuery.isError && tasksQuery.data?.items.length === 0 && (
          <div className="empty-state">
            <h3>No tasks yet</h3>
            <p>Add a task and its due date to see how much time remains.</p>
          </div>
        )}

        {tasksQuery.data && tasksQuery.data.items.length > 0 && (
          <ul className="task-list">
            {tasksQuery.data.items.map((task) => <TaskListItem key={task.id} task={task} />)}
          </ul>
        )}
      </section>
    </main>
  );
}
