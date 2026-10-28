package com.jodngan.task.infrastructure;

import java.time.Instant;
import java.util.UUID;

import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
class TaskEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskEntity() {
    }

    private TaskEntity(Task task) {
        id = task.id();
        title = task.title();
        dueAt = task.dueAt();
        status = task.status();
        completedAt = task.completedAt();
        createdAt = task.createdAt();
        updatedAt = task.updatedAt();
    }

    static TaskEntity from(Task task) {
        return new TaskEntity(task);
    }

    Task toDomain() {
        return new Task(id, title, dueAt, status, completedAt, createdAt, updatedAt);
    }
}
