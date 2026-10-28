package com.jodngan.task.api;

import java.time.Instant;
import java.util.UUID;

import com.jodngan.task.application.TaskView;
import com.jodngan.task.domain.TaskStatus;

public record TaskResponse(
        UUID id,
        String title,
        Instant dueAt,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        Planning planning) {

    static TaskResponse from(TaskView view) {
        var task = view.task();
        return new TaskResponse(
                task.id(),
                task.title(),
                task.dueAt(),
                task.status(),
                task.createdAt(),
                task.updatedAt(),
                new Planning(view.planning().remainingSeconds(), view.planning().overdue()));
    }

    public record Planning(long remainingSeconds, boolean overdue) {
    }
}
