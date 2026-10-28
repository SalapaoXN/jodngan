package com.jodngan.task.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jodngan.task.domain.Task;

public interface TaskRepository {

    Task save(Task task);

    List<Task> findActiveTasks();

    Optional<Task> findById(UUID id);
}
