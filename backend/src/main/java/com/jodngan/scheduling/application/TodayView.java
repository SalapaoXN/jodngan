package com.jodngan.scheduling.application;

import java.time.Instant;
import java.util.List;

import com.jodngan.scheduling.domain.RecommendationReason;
import com.jodngan.scheduling.domain.RiskAssessment;
import com.jodngan.scheduling.domain.StartGuidance;
import com.jodngan.scheduling.domain.UrgencyBand;
import com.jodngan.task.application.TaskPlanningData;

public record TodayView(
        Instant generatedAt,
        Recommendation primaryRecommendation,
        List<Recommendation> upcomingTasks) {

    public TodayView {
        upcomingTasks = List.copyOf(upcomingTasks);
    }

    public record Recommendation(
            TaskPlanningData task,
            UrgencyBand urgency,
            RecommendationReason reasonCode,
            StartGuidance startGuidance,
            RiskAssessment risk,
            long remainingSeconds,
            boolean overdue) {

        public String reason() {
            return reasonCode.message();
        }
    }
}
