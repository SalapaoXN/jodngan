import { apiFetch } from '../../lib/api/client';
import type { Course, CourseListResponse } from './types';

export function getCourses(): Promise<CourseListResponse> {
  return apiFetch<CourseListResponse>('/api/courses');
}

export function createCourse(name: string): Promise<Course> {
  return apiFetch<Course>('/api/courses', {
    method: 'POST',
    body: JSON.stringify({ name }),
  });
}

export function renameCourse(courseId: string, name: string): Promise<Course> {
  return apiFetch<Course>(`/api/courses/${courseId}`, {
    method: 'PUT',
    body: JSON.stringify({ name }),
  });
}

export function removeCourse(courseId: string): Promise<void> {
  return apiFetch<void>(`/api/courses/${courseId}`, { method: 'DELETE' });
}
