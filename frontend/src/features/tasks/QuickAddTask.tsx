import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';

import { todayQueryKey } from '../today/queries';
import { createTask } from './api';
import { DateTimePicker } from './DateTimePicker';
import { activeTasksQueryKey } from './queries';
import type { TaskListResponse } from './types';

interface QuickAddValues {
  title: string;
  dueAt: string;
}

export function QuickAddTask() {
  const [isOpen, setIsOpen] = useState(false);
  const queryClient = useQueryClient();
  const {
    control,
    handleSubmit,
    register,
    reset,
    formState: { errors },
  } = useForm<QuickAddValues>({ defaultValues: { title: '', dueAt: '' } });

  const createMutation = useMutation({
    mutationFn: createTask,
    onSuccess: (createdTask) => {
      queryClient.setQueryData<TaskListResponse>(activeTasksQueryKey, (current) => ({
        items: [...(current?.items ?? []), createdTask],
      }));
      void queryClient.invalidateQueries({ queryKey: activeTasksQueryKey });
      void queryClient.invalidateQueries({ queryKey: todayQueryKey });
      reset();
      setIsOpen(false);
    },
  });

  function closeForm() {
    createMutation.reset();
    reset();
    setIsOpen(false);
  }

  function submit(values: QuickAddValues) {
    createMutation.mutate({
      title: values.title.trim(),
      dueAt: new Date(values.dueAt).toISOString(),
    });
  }

  return (
    <section className={isOpen ? 'quick-add quick-add-open' : 'quick-add quick-add-closed'} aria-label="Quick Add task">
      {!isOpen ? (
        <div className="quick-add-launcher">
          <div className="quick-add-copy">
            <span className="quick-add-icon" aria-hidden="true">＋</span>
            <div>
              <strong>Capture a new task</strong>
              <span>Just the task and deadline. Add details later.</span>
            </div>
          </div>
          <button type="button" className="primary-action" onClick={() => setIsOpen(true)}>
            Add task
          </button>
        </div>
      ) : (
        <form noValidate onSubmit={handleSubmit(submit)}>
          <div className="quick-add-heading">
            <div>
              <p className="section-kicker">Fast capture</p>
              <h2 id="quick-add-heading">Quick Add</h2>
              <p>Keep it lightweight: name the work and set its deadline.</p>
            </div>
          </div>
          <div className="field-group">
            <label htmlFor="task-title">Task name</label>
            <input
              id="task-title"
              autoFocus
              maxLength={200}
              {...register('title', {
                validate: (value) => value.trim().length > 0 || 'Enter a task name.',
                maxLength: { value: 200, message: 'Use 200 characters or fewer.' },
              })}
              aria-invalid={Boolean(errors.title)}
              aria-describedby={errors.title ? 'task-title-error' : undefined}
            />
            {errors.title && (
              <p className="field-error" id="task-title-error" role="alert">
                {errors.title.message}
              </p>
            )}
          </div>

          <div className="field-group">
            <label htmlFor="task-due-at">Due date and time</label>
            <Controller
              name="dueAt"
              control={control}
              rules={{ required: 'Choose a due date and time.' }}
              render={({ field }) => (
                <DateTimePicker
                  id="task-due-at"
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  invalid={Boolean(errors.dueAt)}
                  describedBy={errors.dueAt ? 'task-due-at-error' : undefined}
                />
              )}
            />
            {errors.dueAt && (
              <p className="field-error" id="task-due-at-error" role="alert">
                {errors.dueAt.message}
              </p>
            )}
          </div>

          {createMutation.isError && (
            <p className="form-error" role="alert">
              {createMutation.error.message}. Your task is still here; try saving again.
            </p>
          )}

          <div className="form-actions">
            <button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Saving…' : 'Save task'}
            </button>
            <button type="button" className="secondary-button" onClick={closeForm}>
              Cancel
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
