package com.jodngan.task.application;

import java.time.Instant;
import java.util.UUID;

import com.jodngan.course.application.CourseData;
import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskImportance;

public record TaskPlanningData(
        UUID id,
        String title,
        Instant dueAt,
        CourseData course,
        Integer estimatedMinutes,
        TaskImportance importance,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskPlanningData from(Task task, CourseData course) {
        return new TaskPlanningData(
                task.id(),
                task.title(),
                task.dueAt(),
                course,
                task.estimatedMinutes(),
                task.importance(),
                task.createdAt(),
                task.updatedAt());
    }
}
