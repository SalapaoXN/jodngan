package com.jodngan.scheduling.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SchedulableTask(
        UUID id,
        String title,
        Instant dueAt,
        Integer estimatedMinutes,
        PlanningImportance importance,
        Instant createdAt,
        Instant updatedAt) {

    public SchedulableTask {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(dueAt, "dueAt is required");
        if (estimatedMinutes != null && estimatedMinutes <= 0) {
            throw new IllegalArgumentException("estimatedMinutes must be greater than zero");
        }
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
    }
}
