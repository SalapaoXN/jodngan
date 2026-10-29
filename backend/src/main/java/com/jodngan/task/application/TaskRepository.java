package com.jodngan.task.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jodngan.task.domain.Task;

public interface TaskRepository {

    Task save(Task task);

    List<Task> findActiveTasks(UUID userId);

    List<Task> findActiveTasks(UUID userId, UUID courseId);

    List<Task> findCompletedTasks(UUID userId);

    List<Task> findCompletedTasks(UUID userId, UUID courseId);

    Optional<Task> findById(UUID userId, UUID id);
}
