package com.jodngan.scheduling.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SchedulableTask(
        UUID id,
        String title,
        Instant dueAt,
        Instant createdAt,
        Instant updatedAt) {

    public SchedulableTask {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(dueAt, "dueAt is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
    }
}
