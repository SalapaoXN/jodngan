package com.jodngan.task.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jodngan.course.application.CourseData;
import com.jodngan.course.application.CourseLookup;
import com.jodngan.course.application.CourseNotFoundException;
import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskImportance;
import com.jodngan.task.domain.TaskStatus;

@Service
public class TaskApplicationService implements TaskPlanningReader {

    private final TaskRepository taskRepository;
    private final CourseLookup courseLookup;
    private final Clock clock;

    public TaskApplicationService(TaskRepository taskRepository, CourseLookup courseLookup, Clock clock) {
        this.taskRepository = taskRepository;
        this.courseLookup = courseLookup;
        this.clock = clock;
    }

    public TaskView createTask(String title, Instant dueAt, UUID courseId) {
        Instant now = clock.instant();
        validateCourse(courseId);
        return view(taskRepository.save(Task.create(title, dueAt, courseId, now)), now);
    }

    public List<TaskView> findTasks(TaskStatus status, UUID courseId) {
        Instant now = clock.instant();
        List<Task> tasks = status == TaskStatus.COMPLETED
                ? (courseId == null ? taskRepository.findCompletedTasks() : taskRepository.findCompletedTasks(courseId))
                : (courseId == null ? taskRepository.findActiveTasks() : taskRepository.findActiveTasks(courseId));
        return tasks.stream()
                .map(task -> view(task, now))
                .toList();
    }

    public TaskView findTask(UUID id) {
        Instant now = clock.instant();
        Task task = taskRepository.findById(id).orElseThrow(TaskNotFoundException::new);
        return view(task, now);
    }

    public TaskView updateTask(
            UUID id,
            String title,
            Instant dueAt,
            UUID courseId,
            Integer estimatedMinutes,
            TaskImportance importance,
            String notes) {
        Instant now = clock.instant();
        validateCourse(courseId);
        Task task = requireTask(id).edit(title, dueAt, courseId, estimatedMinutes, importance, notes, now);
        return view(taskRepository.save(task), now);
    }

    public TaskView completeTask(UUID id) {
        Instant now = clock.instant();
        Task task = requireTask(id).complete(now);
        return view(taskRepository.save(task), now);
    }

    public TaskView reopenTask(UUID id) {
        Instant now = clock.instant();
        Task task = requireTask(id).reopen(now);
        return view(taskRepository.save(task), now);
    }

    @Override
    public List<TaskPlanningData> findActivePlanningTasks() {
        return taskRepository.findActiveTasks().stream()
                .map(task -> TaskPlanningData.from(task, resolveCourse(task.courseId())))
                .toList();
    }

    private Task requireTask(UUID id) {
        return taskRepository.findById(id).orElseThrow(TaskNotFoundException::new);
    }

    private TaskView view(Task task, Instant now) {
        return new TaskView(task, resolveCourse(task.courseId()), task.deadlineProgressAt(now));
    }

    private void validateCourse(UUID courseId) {
        if (courseId != null && courseLookup.findCourse(courseId).isEmpty()) {
            throw new CourseNotFoundException();
        }
    }

    private CourseData resolveCourse(UUID courseId) {
        if (courseId == null) {
            return null;
        }
        return courseLookup.findCourse(courseId).orElse(null);
    }
}
