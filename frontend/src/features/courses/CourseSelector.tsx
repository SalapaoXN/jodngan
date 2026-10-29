import { useCourses } from './queries';

interface CourseSelectorProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  label?: string;
  describedBy?: string;
}

export function CourseSelector({
  id,
  value,
  onChange,
  onBlur,
  label = 'Course',
  describedBy,
}: CourseSelectorProps) {
  const coursesQuery = useCourses();

  return (
    <div className="field-group course-selector-field">
      <label htmlFor={id}>
        {label} <span className="field-optional" aria-hidden="true">Optional</span>
      </label>
      <select
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onBlur={onBlur}
        aria-describedby={describedBy ?? `${id}-help`}
      >
        <option value="">No course</option>
        {coursesQuery.data?.items.map((course) => (
          <option key={course.id} value={course.id}>{course.name}</option>
        ))}
      </select>
      <p className="field-help" id={`${id}-help`}>
        {coursesQuery.isPending
          ? 'Loading courses… You can still save without one.'
          : coursesQuery.isError
            ? 'Courses are unavailable right now. You can still save without one.'
            : coursesQuery.data?.items.length
              ? 'Use a course only when it helps you organize the work.'
              : 'No courses yet. You can create them from the Tasks page.'}
      </p>
    </div>
  );
}
