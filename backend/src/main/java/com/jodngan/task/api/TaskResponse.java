package com.jodngan.task.api;

import java.time.Instant;
import java.util.UUID;

import com.jodngan.scheduling.domain.RiskAssessment;
import com.jodngan.scheduling.domain.RiskEvaluator;
import com.jodngan.scheduling.domain.RiskReason;
import com.jodngan.scheduling.domain.RiskState;
import com.jodngan.task.application.TaskView;
import com.jodngan.task.domain.TaskImportance;
import com.jodngan.task.domain.TaskStatus;

public record TaskResponse(
        UUID id,
        String title,
        Instant dueAt,
        CourseSummary course,
        Integer estimatedMinutes,
        TaskImportance importance,
        String notes,
        TaskStatus status,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        Planning planning) {

    static TaskResponse from(TaskView view) {
        var task = view.task();
        return new TaskResponse(
                task.id(),
                task.title(),
                task.dueAt(),
                view.course() == null ? null : CourseSummary.from(view.course()),
                task.estimatedMinutes(),
                task.importance(),
                task.notes(),
                task.status(),
                task.completedAt(),
                task.createdAt(),
                task.updatedAt(),
                new Planning(
                        view.planning().remainingSeconds(),
                        view.planning().overdue(),
                        task.status() == TaskStatus.COMPLETED
                                ? null
                                : Risk.from(RiskEvaluator.assess(
                                        task.estimatedMinutes(),
                                        view.planning().remainingSeconds(),
                                        view.planning().overdue()))));
    }

    public record CourseSummary(UUID id, String name) {
        public static CourseSummary from(com.jodngan.course.application.CourseData course) {
            return new CourseSummary(course.id(), course.name());
        }
    }

    public record Planning(long remainingSeconds, boolean overdue, Risk risk) {
    }

    public record Risk(RiskState state, RiskReason reasonCode, String message) {

        public static Risk from(RiskAssessment assessment) {
            return new Risk(assessment.state(), assessment.reasonCode(), assessment.message());
        }
    }
}
