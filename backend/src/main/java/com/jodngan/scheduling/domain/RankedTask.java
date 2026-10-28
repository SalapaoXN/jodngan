package com.jodngan.scheduling.domain;

public record RankedTask(
        SchedulableTask task,
        UrgencyBand urgency,
        RecommendationReason reason,
        StartGuidance startGuidance,
        long remainingSeconds,
        boolean overdue) {
}
