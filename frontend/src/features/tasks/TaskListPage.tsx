import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { AppHeader } from '../../app/AppHeader';
import { ApiError } from '../../lib/api/client';
import { CourseManager } from '../courses/CourseManager';
import { useCourses } from '../courses/queries';
import { todayQueryKey } from '../today/queries';
import { completeTask, reopenTask } from './lifecycleApi';
import { completedTasksQueryKey, taskDetailQueryKey, useCompletedTasks } from './lifecycleQueries';
import { QuickAddTask } from './QuickAddTask';
import { activeTasksQueryKey, useActiveTasks } from './queries';
import { TaskLifecycleCard } from './TaskLifecycleCard';

export function TaskListPage() {
  const [selectedTab, setSelectedTab] = useState<'ACTIVE' | 'COMPLETED'>('ACTIVE');
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const queryClient = useQueryClient();
  const coursesQuery = useCourses();
  const selectedCourseStillExists = !selectedCourseId
    || !coursesQuery.data
    || coursesQuery.data.items.some((course) => course.id === selectedCourseId);
  const effectiveCourseId = selectedCourseStillExists ? selectedCourseId : '';
  const activeQuery = useActiveTasks(effectiveCourseId || null);
  const completedQuery = useCompletedTasks(effectiveCourseId || null);
  const currentQuery = selectedTab === 'ACTIVE' ? activeQuery : completedQuery;

  async function refreshAfterLifecycleChange(taskId: string) {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: activeTasksQueryKey }),
      queryClient.invalidateQueries({ queryKey: completedTasksQueryKey }),
      queryClient.invalidateQueries({ queryKey: todayQueryKey }),
      queryClient.invalidateQueries({ queryKey: taskDetailQueryKey(taskId) }),
    ]);
  }

  const completeMutation = useMutation({
    mutationFn: completeTask,
    onSuccess: (_, taskId) => refreshAfterLifecycleChange(taskId),
  });
  const reopenMutation = useMutation({
    mutationFn: reopenTask,
    onSuccess: (_, taskId) => refreshAfterLifecycleChange(taskId),
  });

  const lifecycleError = completeMutation.error ?? reopenMutation.error;

  return (
    <main className="page-shell product-shell">
      <AppHeader active="tasks" />

      <header className="today-header page-hero compact-header">
        <div>
          <p className="eyebrow">Task library</p>
          <h1>Tasks</h1>
          <p>Keep active work visible, archive completed work, and reopen anything that needs another pass.</p>
        </div>
      </header>

      <QuickAddTask />
      <CourseManager />

      <section className="task-section content-block" aria-labelledby="task-list-heading">
        <div className="section-heading section-heading-copy">
          <div>
            <p className="section-kicker">Task status</p>
            <h2 id="task-list-heading">Your tasks</h2>
          </div>
          {currentQuery.data && <span className="count-badge">{currentQuery.data.items.length}</span>}
        </div>

        <div className="task-list-controls">
          <div className="course-filter">
            <label htmlFor="task-course-filter">Course</label>
            <select
              id="task-course-filter"
              value={effectiveCourseId}
              onChange={(event) => setSelectedCourseId(event.target.value)}
            >
              <option value="">All courses</option>
              {coursesQuery.data?.items.map((course) => (
                <option key={course.id} value={course.id}>{course.name}</option>
              ))}
            </select>
          </div>

          <div className="task-tabs" role="tablist" aria-label="Task status">
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'ACTIVE'}
              className={selectedTab === 'ACTIVE' ? 'tab-button active' : 'tab-button'}
              onClick={() => setSelectedTab('ACTIVE')}
            >
              Active
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'COMPLETED'}
              className={selectedTab === 'COMPLETED' ? 'tab-button active' : 'tab-button'}
              onClick={() => setSelectedTab('COMPLETED')}
            >
              Completed
            </button>
          </div>
        </div>

        {currentQuery.isPending && <p role="status">Loading tasks…</p>}

        {currentQuery.isError && (
          <div className="error-state" role="alert">
            <p>
              {currentQuery.error instanceof ApiError
                ? currentQuery.error.message
                : 'Tasks could not be loaded.'}
            </p>
            <button type="button" className="secondary-button" onClick={() => void currentQuery.refetch()}>
              Try again
            </button>
          </div>
        )}

        {lifecycleError && (
          <div className="error-state" role="alert">
            <p>{lifecycleError.message}</p>
          </div>
        )}

        {!currentQuery.isPending && !currentQuery.isError && currentQuery.data?.items.length === 0 && (
          <div className="empty-state">
            <h3>{selectedTab === 'ACTIVE' ? 'No active tasks' : 'No completed tasks'}</h3>
            <p>
              {selectedTab === 'ACTIVE'
                ? 'Add a task to start planning what to work on next.'
                : 'Completed tasks will appear here.'}
            </p>
          </div>
        )}

        {currentQuery.data && currentQuery.data.items.length > 0 && (
          <ul className="task-list">
            {currentQuery.data.items.map((task) => (
              <TaskLifecycleCard
                key={task.id}
                task={task}
                actionLabel={task.status === 'ACTIVE' ? 'Complete' : 'Reopen'}
                actionPending={
                  task.status === 'ACTIVE'
                    ? completeMutation.isPending && completeMutation.variables === task.id
                    : reopenMutation.isPending && reopenMutation.variables === task.id
                }
                onAction={() => {
                  if (task.status === 'ACTIVE') {
                    completeMutation.mutate(task.id);
                  } else {
                    reopenMutation.mutate(task.id);
                  }
                }}
              />
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
