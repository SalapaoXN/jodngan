package com.jodngan.task.application;

import java.time.Instant;
import java.util.UUID;

import com.jodngan.task.domain.Task;

public record TaskPlanningData(
        UUID id,
        String title,
        Instant dueAt,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskPlanningData from(Task task) {
        return new TaskPlanningData(
                task.id(),
                task.title(),
                task.dueAt(),
                task.createdAt(),
                task.updatedAt());
    }
}
