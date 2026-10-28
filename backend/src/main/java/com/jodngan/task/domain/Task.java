package com.jodngan.task.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Task(
        UUID id,
        String title,
        Instant dueAt,
        TaskStatus status,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {

    public Task {
        Objects.requireNonNull(id, "id is required");
        title = Objects.requireNonNull(title, "title is required").strip();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("title must not exceed 200 characters");
        }
        Objects.requireNonNull(dueAt, "dueAt is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");

        if ((status == TaskStatus.ACTIVE) != (completedAt == null)) {
            throw new IllegalArgumentException("completedAt must match task status");
        }
    }

    public static Task create(String title, Instant dueAt, Instant now) {
        return new Task(UUID.randomUUID(), title, dueAt, TaskStatus.ACTIVE, null, now, now);
    }

    public TaskDeadlineProgress deadlineProgressAt(Instant now) {
        long remainingSeconds = Duration.between(now, dueAt).getSeconds();
        return new TaskDeadlineProgress(remainingSeconds, dueAt.isBefore(now));
    }
}
