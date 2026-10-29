package com.jodngan.task.api;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank(message = "Task name is required")
        @Size(max = 200, message = "Task name must be 200 characters or fewer")
        String title,
        @NotNull(message = "Due date and time is required")
        Instant dueAt,
        UUID courseId) {
}
