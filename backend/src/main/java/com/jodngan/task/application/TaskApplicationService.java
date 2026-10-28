package com.jodngan.task.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jodngan.task.domain.Task;

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

    public List<TaskView> findActiveTasks() {
        Instant now = clock.instant();
        return taskRepository.findActiveTasks().stream()
                .map(task -> view(task, now))
                .toList();
    }

    public TaskView findTask(UUID id) {
        Instant now = clock.instant();
        Task task = taskRepository.findById(id).orElseThrow(TaskNotFoundException::new);
        return view(task, now);
    }

    @Override
    public List<TaskPlanningData> findActivePlanningTasks() {
        return taskRepository.findActiveTasks().stream()
                .map(TaskPlanningData::from)
                .toList();
    }

    private TaskView view(Task task, Instant now) {
        return new TaskView(task, task.deadlineProgressAt(now));
    }
}
