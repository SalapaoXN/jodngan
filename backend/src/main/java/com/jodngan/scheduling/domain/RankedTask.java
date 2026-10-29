package com.jodngan.scheduling.domain;

public record RankedTask(
        SchedulableTask task,
        UrgencyBand urgency,
        int recommendationScore,
        Double effortPressureRatio,
        RecommendationReason reason,
        StartGuidance startGuidance,
        RiskAssessment risk,
        long remainingSeconds,
        boolean overdue) {
}
