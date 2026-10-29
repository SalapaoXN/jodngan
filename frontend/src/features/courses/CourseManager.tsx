import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

import { todayQueryKey } from '../today/queries';
import { createCourse, removeCourse, renameCourse } from './api';
import { coursesQueryKey, useCourses } from './queries';

export function CourseManager() {
  const [isOpen, setIsOpen] = useState(false);
  const [newName, setNewName] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [confirmRemoveId, setConfirmRemoveId] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const coursesQuery = useCourses();

  async function refreshCourseReferences() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: coursesQueryKey }),
      queryClient.invalidateQueries({ queryKey: ['tasks'] }),
      queryClient.invalidateQueries({ queryKey: todayQueryKey }),
    ]);
  }

  const createMutation = useMutation({
    mutationFn: createCourse,
    onSuccess: async () => {
      setNewName('');
      await queryClient.invalidateQueries({ queryKey: coursesQueryKey });
    },
  });
  const renameMutation = useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) => renameCourse(id, name),
    onSuccess: async () => {
      setEditingId(null);
      setEditName('');
      await refreshCourseReferences();
    },
  });
  const removeMutation = useMutation({
    mutationFn: removeCourse,
    onSuccess: async () => {
      setConfirmRemoveId(null);
      await refreshCourseReferences();
    },
  });

  const mutationError = createMutation.error ?? renameMutation.error ?? removeMutation.error;

  return (
    <section className="course-manager" aria-label="Course manager">
      <button
        type="button"
        className="course-manager-toggle secondary-button"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((current) => !current)}
      >
        {isOpen ? 'Close courses' : 'Manage courses'}
      </button>

      {isOpen && (
        <div className="course-manager-panel">
          <div className="course-manager-heading">
            <div>
              <p className="section-kicker">Optional organization</p>
              <h3>Courses</h3>
              <p>Group tasks when it helps. Removing a course never removes its tasks.</p>
            </div>
          </div>

          <form
            className="course-create-form"
            onSubmit={(event) => {
              event.preventDefault();
              const trimmed = newName.trim();
              if (trimmed) createMutation.mutate(trimmed);
            }}
          >
            <label htmlFor="new-course-name">New course</label>
            <div className="course-create-row">
              <input
                id="new-course-name"
                value={newName}
                maxLength={120}
                placeholder="e.g. Data Warehouse"
                onChange={(event) => setNewName(event.target.value)}
              />
              <button type="submit" disabled={createMutation.isPending || newName.trim().length === 0}>
                {createMutation.isPending ? 'Adding…' : 'Add course'}
              </button>
            </div>
          </form>

          {mutationError && <p className="form-error" role="alert">{mutationError.message}</p>}
          {coursesQuery.isPending && <p role="status">Loading courses…</p>}
          {coursesQuery.isError && <p className="form-error" role="alert">Courses could not be loaded.</p>}

          {coursesQuery.data && coursesQuery.data.items.length === 0 && (
            <p className="course-empty">No courses yet. Tasks can still be created without one.</p>
          )}

          {coursesQuery.data && coursesQuery.data.items.length > 0 && (
            <ul className="course-list">
              {coursesQuery.data.items.map((course) => (
                <li key={course.id} className="course-row">
                  {editingId === course.id ? (
                    <div className="course-edit-row">
                      <input
                        aria-label={`Rename ${course.name}`}
                        value={editName}
                        maxLength={120}
                        onChange={(event) => setEditName(event.target.value)}
                      />
                      <button
                        type="button"
                        disabled={renameMutation.isPending || editName.trim().length === 0}
                        onClick={() => renameMutation.mutate({ id: course.id, name: editName.trim() })}
                      >
                        Save
                      </button>
                      <button type="button" className="secondary-button" onClick={() => setEditingId(null)}>
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <>
                      <span className="course-name">{course.name}</span>
                      <div className="course-row-actions">
                        <button
                          type="button"
                          className="text-button"
                          onClick={() => {
                            setEditingId(course.id);
                            setEditName(course.name);
                            setConfirmRemoveId(null);
                          }}
                        >
                          Rename
                        </button>
                        {confirmRemoveId === course.id ? (
                          <>
                            <button
                              type="button"
                              className="danger-button"
                              disabled={removeMutation.isPending}
                              onClick={() => removeMutation.mutate(course.id)}
                            >
                              {removeMutation.isPending ? 'Removing…' : 'Confirm remove'}
                            </button>
                            <button type="button" className="text-button" onClick={() => setConfirmRemoveId(null)}>
                              Cancel
                            </button>
                          </>
                        ) : (
                          <button
                            type="button"
                            className="text-button danger-text"
                            onClick={() => {
                              setConfirmRemoveId(course.id);
                              setEditingId(null);
                            }}
                          >
                            Remove
                          </button>
                        )}
                      </div>
                    </>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}
