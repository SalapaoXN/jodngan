package com.jodngan.scheduling.api;

import java.time.Instant;
import java.util.List;

import com.jodngan.scheduling.application.TodayView;
import com.jodngan.scheduling.domain.RecommendationReason;
import com.jodngan.scheduling.domain.StartGuidance;
import com.jodngan.scheduling.domain.UrgencyBand;
import com.jodngan.task.api.TaskResponse;
import com.jodngan.task.domain.TaskStatus;

public record TodayResponse(
        Instant generatedAt,
        Recommendation primaryRecommendation,
        List<Recommendation> upcomingTasks) {

    static TodayResponse from(TodayView view) {
        return new TodayResponse(
                view.generatedAt(),
                view.primaryRecommendation() == null ? null : Recommendation.from(view.primaryRecommendation()),
                view.upcomingTasks().stream().map(Recommendation::from).toList());
    }

    public record Recommendation(
            TaskResponse task,
            UrgencyBand urgency,
            RecommendationReason reasonCode,
            String reason,
            StartGuidance startGuidance) {

        static Recommendation from(TodayView.Recommendation recommendation) {
            var task = recommendation.task();
            TaskResponse taskResponse = new TaskResponse(
                    task.id(),
                    task.title(),
                    task.dueAt(),
                    TaskStatus.ACTIVE,
                    task.createdAt(),
                    task.updatedAt(),
                    new TaskResponse.Planning(
                            recommendation.remainingSeconds(),
                            recommendation.overdue()));
            return new Recommendation(
                    taskResponse,
                    recommendation.urgency(),
                    recommendation.reasonCode(),
                    recommendation.reason(),
                    recommendation.startGuidance());
        }
    }
}
