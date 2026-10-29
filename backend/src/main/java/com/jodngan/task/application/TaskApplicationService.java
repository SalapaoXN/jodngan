package com.jodngan.task.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskImportance;
import com.jodngan.task.domain.TaskStatus;

@Service
public class TaskApplicationService implements TaskPlanningReader {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public TaskApplicationService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    public TaskView createTask(String title, Instant dueAt) {
        Instant now = clock.instant();
        return view(taskRepository.save(Task.create(title, dueAt, now)), now);
    }

    public List<TaskView> findTasks(TaskStatus status) {
        Instant now = clock.instant();
        List<Task> tasks = status == TaskStatus.COMPLETED
                ? taskRepository.findCompletedTasks()
                : taskRepository.findActiveTasks();
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
            Integer estimatedMinutes,
            TaskImportance importance,
            String notes) {
        Instant now = clock.instant();
        Task task = requireTask(id).edit(title, dueAt, estimatedMinutes, importance, notes, now);
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
                .map(TaskPlanningData::from)
                .toList();
    }

    private Task requireTask(UUID id) {
        return taskRepository.findById(id).orElseThrow(TaskNotFoundException::new);
    }

    private TaskView view(Task task, Instant now) {
        return new TaskView(task, task.deadlineProgressAt(now));
    }
}
