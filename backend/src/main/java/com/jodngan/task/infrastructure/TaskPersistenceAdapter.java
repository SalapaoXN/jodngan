package com.jodngan.task.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.jodngan.task.application.TaskRepository;
import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskStatus;

@Repository
public class TaskPersistenceAdapter implements TaskRepository {

    private final SpringDataTaskRepository repository;

    public TaskPersistenceAdapter(SpringDataTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public Task save(Task task) {
        return repository.save(TaskEntity.from(task)).toDomain();
    }

    @Override
    public List<Task> findActiveTasks() {
        return repository.findByStatusOrderByDueAtAscIdAsc(TaskStatus.ACTIVE).stream()
                .map(TaskEntity::toDomain)
                .toList();
    }

    @Override
    public List<Task> findCompletedTasks() {
        return repository.findByStatusOrderByCompletedAtDescIdAsc(TaskStatus.COMPLETED).stream()
                .map(TaskEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return repository.findById(id).map(TaskEntity::toDomain);
    }
}
