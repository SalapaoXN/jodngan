package com.jodngan.task.application;

import java.time.Instant;
import java.util.UUID;

import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskImportance;

public record TaskPlanningData(
        UUID id,
        String title,
        Instant dueAt,
        Integer estimatedMinutes,
        TaskImportance importance,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskPlanningData from(Task task) {
        return new TaskPlanningData(
                task.id(),
                task.title(),
                task.dueAt(),
                task.estimatedMinutes(),
                task.importance(),
                task.createdAt(),
                task.updatedAt());
    }
}
