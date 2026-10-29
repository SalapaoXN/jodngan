package com.jodngan.task.api;

import java.time.Instant;

import com.jodngan.task.domain.TaskImportance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank(message = "Task name is required")
        @Size(max = 200, message = "Task name must not exceed 200 characters")
        String title,
        @NotNull(message = "Due date and time is required")
        Instant dueAt,
        @Positive(message = "Estimated effort must be greater than zero")
        Integer estimatedMinutes,
        TaskImportance importance,
        String notes) {
}
