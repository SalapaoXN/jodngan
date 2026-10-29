package com.jodngan.task.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jodngan.course.application.CourseData;
import com.jodngan.course.application.CourseLookup;
import com.jodngan.course.application.CourseNotFoundException;
import com.jodngan.identity.application.CurrentUserProvider;
import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskImportance;
import com.jodngan.task.domain.TaskStatus;

@Service
public class TaskApplicationService implements TaskPlanningReader {

    private final TaskRepository taskRepository;
    private final CourseLookup courseLookup;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public TaskApplicationService(
            TaskRepository taskRepository,
            CourseLookup courseLookup,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.courseLookup = courseLookup;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public TaskView createTask(String title, Instant dueAt, UUID courseId) {
        Instant now = clock.instant();
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        validateCourse(userId, courseId);
        return view(taskRepository.save(Task.create(userId, title, dueAt, courseId, now)), now);
    }

    public List<TaskView> findTasks(TaskStatus status, UUID courseId) {
        Instant now = clock.instant();
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        validateCourse(userId, courseId);
        List<Task> tasks = status == TaskStatus.COMPLETED
                ? (courseId == null ? taskRepository.findCompletedTasks(userId) : taskRepository.findCompletedTasks(userId, courseId))
                : (courseId == null ? taskRepository.findActiveTasks(userId) : taskRepository.findActiveTasks(userId, courseId));
        return tasks.stream()
                .map(task -> view(task, now))
                .toList();
    }

    public TaskView findTask(UUID id) {
        Instant now = clock.instant();
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        Task task = taskRepository.findById(userId, id).orElseThrow(TaskNotFoundException::new);
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
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        validateCourse(userId, courseId);
        Task task = requireTask(userId, id).edit(title, dueAt, courseId, estimatedMinutes, importance, notes, now);
        return view(taskRepository.save(task), now);
    }

    public TaskView completeTask(UUID id) {
        Instant now = clock.instant();
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        Task task = requireTask(userId, id).complete(now);
        return view(taskRepository.save(task), now);
    }

    public TaskView reopenTask(UUID id) {
        Instant now = clock.instant();
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        Task task = requireTask(userId, id).reopen(now);
        return view(taskRepository.save(task), now);
    }

    @Override
    public List<TaskPlanningData> findActivePlanningTasks() {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return taskRepository.findActiveTasks(userId).stream()
                .map(task -> TaskPlanningData.from(task, resolveCourse(userId, task.courseId())))
                .toList();
    }

    private Task requireTask(UUID userId, UUID id) {
        return taskRepository.findById(userId, id).orElseThrow(TaskNotFoundException::new);
    }

    private TaskView view(Task task, Instant now) {
        return new TaskView(task, resolveCourse(task.userId(), task.courseId()), task.deadlineProgressAt(now));
    }

    private void validateCourse(UUID userId, UUID courseId) {
        if (courseId != null && courseLookup.findCourse(userId, courseId).isEmpty()) {
            throw new CourseNotFoundException();
        }
    }

    private CourseData resolveCourse(UUID userId, UUID courseId) {
        if (courseId == null) {
            return null;
        }
        return courseLookup.findCourse(userId, courseId).orElse(null);
    }
}
