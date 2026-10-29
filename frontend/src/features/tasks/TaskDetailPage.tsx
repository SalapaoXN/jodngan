import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link, useParams } from 'react-router';

import { AppHeader } from '../../app/AppHeader';
import { ApiError } from '../../lib/api/client';
import { todayQueryKey } from '../today/queries';
import { DateTimePicker } from './DateTimePicker';
import { formatRemainingTime } from './deadlineLabel';
import { completeTask, reopenTask, updateTask } from './lifecycleApi';
import { completedTasksQueryKey, taskDetailQueryKey, useTaskDetail } from './lifecycleQueries';
import { activeTasksQueryKey } from './queries';
import type { Task } from './types';

interface TaskDetailValues {
  title: string;
  dueAt: string;
}

function toLocalDateTimeInput(iso: string): string {
  const date = new Date(iso);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function TaskDetailPage() {
  const { taskId = '' } = useParams();
  const queryClient = useQueryClient();
  const taskQuery = useTaskDetail(taskId);
  const {
    control,
    handleSubmit,
    register,
    reset,
    formState: { errors },
  } = useForm<TaskDetailValues>();

  useEffect(() => {
    if (taskQuery.data) {
      reset({
        title: taskQuery.data.title,
        dueAt: toLocalDateTimeInput(taskQuery.data.dueAt),
      });
    }
  }, [reset, taskQuery.data]);

  async function syncTask(task: Task) {
    queryClient.setQueryData(taskDetailQueryKey(task.id), task);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: activeTasksQueryKey }),
      queryClient.invalidateQueries({ queryKey: completedTasksQueryKey }),
      queryClient.invalidateQueries({ queryKey: todayQueryKey }),
    ]);
  }

  const updateMutation = useMutation({
    mutationFn: (values: TaskDetailValues) => updateTask(taskId, {
      title: values.title.trim(),
      dueAt: new Date(values.dueAt).toISOString(),
    }),
    onSuccess: syncTask,
  });
  const completeMutation = useMutation({
    mutationFn: () => completeTask(taskId),
    onSuccess: syncTask,
  });
  const reopenMutation = useMutation({
    mutationFn: () => reopenTask(taskId),
    onSuccess: syncTask,
  });

  if (taskQuery.isPending) {
    return (
      <main className="page-shell product-shell">
        <AppHeader active="tasks" />
        <div className="content-block loading-card"><p role="status">Loading task…</p></div>
      </main>
    );
  }

  if (taskQuery.isError || !taskQuery.data) {
    return (
      <main className="page-shell product-shell">
        <AppHeader active="tasks" />
        <Link className="back-link" to="/tasks">← Back to Tasks</Link>
        <div className="error-state content-block" role="alert">
          <p>
            {taskQuery.error instanceof ApiError
              ? taskQuery.error.message
              : 'Task could not be loaded.'}
          </p>
          <button type="button" className="secondary-button" onClick={() => void taskQuery.refetch()}>
            Try again
          </button>
        </div>
      </main>
    );
  }

  const task = taskQuery.data;
  const lifecycleMutation = task.status === 'ACTIVE' ? completeMutation : reopenMutation;
  const mutationError = updateMutation.error ?? completeMutation.error ?? reopenMutation.error;

  return (
    <main className="page-shell product-shell">
      <AppHeader active="tasks" />

      <div className="detail-breadcrumb-row">
        <Link className="back-link" to="/tasks">← Back to Tasks</Link>
        <span className={task.status === 'ACTIVE' ? 'status-pill active' : 'status-pill completed'}>
          {task.status === 'ACTIVE' ? 'Active' : 'Completed'}
        </span>
      </div>

      <header className="today-header page-hero detail-hero compact-header">
        <div>
          <p className="eyebrow">Task detail</p>
          <h1>{task.title}</h1>
          <p>{task.status === 'ACTIVE' ? 'Update the deadline or mark this work complete.' : 'This task is complete, but you can reopen it anytime.'}</p>
        </div>
      </header>

      <section className="task-detail-card content-block" aria-labelledby="edit-task-heading">
        <div className="section-heading section-heading-copy">
          <div>
            <p className="section-kicker">Task information</p>
            <h2 id="edit-task-heading">Edit task</h2>
          </div>
        </div>

        <form noValidate onSubmit={handleSubmit((values) => updateMutation.mutate(values))}>
          <div className="field-group">
            <label htmlFor="detail-task-title">Task name</label>
            <input
              id="detail-task-title"
              maxLength={200}
              {...register('title', {
                validate: (value) => value.trim().length > 0 || 'Enter a task name.',
                maxLength: { value: 200, message: 'Use 200 characters or fewer.' },
              })}
              aria-invalid={Boolean(errors.title)}
              aria-describedby={errors.title ? 'detail-task-title-error' : undefined}
            />
            {errors.title && <p id="detail-task-title-error" className="field-error" role="alert">{errors.title.message}</p>}
          </div>

          <div className="field-group">
            <label htmlFor="detail-task-due-at">Due date and time</label>
            <Controller
              name="dueAt"
              control={control}
              rules={{ required: 'Choose a due date and time.' }}
              render={({ field }) => (
                <DateTimePicker
                  id="detail-task-due-at"
                  value={field.value ?? ''}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  invalid={Boolean(errors.dueAt)}
                  describedBy={errors.dueAt ? 'detail-task-due-error' : undefined}
                />
              )}
            />
            {errors.dueAt && <p id="detail-task-due-error" className="field-error" role="alert">{errors.dueAt.message}</p>}
          </div>

          {task.status === 'ACTIVE' ? (
            <p className={task.planning.overdue ? 'deadline overdue' : 'deadline'}>
              {formatRemainingTime(task.planning.remainingSeconds, task.planning.overdue)}
            </p>
          ) : (
            <p className="completed-label">Completed tasks are excluded from Today recommendations.</p>
          )}

          {mutationError && <p className="form-error" role="alert">{mutationError.message}</p>}

          <div className="form-actions">
            <button type="submit" disabled={updateMutation.isPending}>
              {updateMutation.isPending ? 'Saving…' : 'Save changes'}
            </button>
            <button
              type="button"
              className="secondary-button"
              disabled={lifecycleMutation.isPending}
              onClick={() => lifecycleMutation.mutate()}
            >
              {lifecycleMutation.isPending
                ? 'Saving…'
                : task.status === 'ACTIVE' ? 'Mark complete' : 'Reopen task'}
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}
