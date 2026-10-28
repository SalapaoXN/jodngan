package com.jodngan.scheduling.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class SchedulingEngine {

    private static final long ONE_DAY_SECONDS = Duration.ofDays(1).toSeconds();
    private static final long THREE_DAYS_SECONDS = Duration.ofDays(3).toSeconds();
    private static final long SEVEN_DAYS_SECONDS = Duration.ofDays(7).toSeconds();

    private static final Comparator<RankedTask> RANKING = Comparator
            .comparingInt((RankedTask task) -> task.urgency().baseScore())
            .reversed()
            .thenComparing(task -> task.task().dueAt())
            .thenComparing(task -> task.task().createdAt())
            .thenComparing(task -> task.task().id().toString());

    public SchedulingResult rank(List<SchedulableTask> tasks, Instant now) {
        List<RankedTask> rankedTasks = tasks.stream()
                .map(task -> evaluate(task, now))
                .sorted(RANKING)
                .toList();
        return new SchedulingResult(rankedTasks);
    }

    private RankedTask evaluate(SchedulableTask task, Instant now) {
        long remainingSeconds = Duration.between(now, task.dueAt()).getSeconds();
        boolean overdue = task.dueAt().isBefore(now);
        UrgencyBand urgency = urgencyFor(remainingSeconds, overdue);
        RecommendationReason reason = reasonFor(urgency);
        StartGuidance startGuidance = guidanceFor(urgency);
        return new RankedTask(task, urgency, reason, startGuidance, remainingSeconds, overdue);
    }

    private UrgencyBand urgencyFor(long remainingSeconds, boolean overdue) {
        if (overdue) {
            return UrgencyBand.OVERDUE;
        }
        if (remainingSeconds <= ONE_DAY_SECONDS) {
            return UrgencyBand.CRITICAL;
        }
        if (remainingSeconds <= THREE_DAYS_SECONDS) {
            return UrgencyBand.HIGH;
        }
        if (remainingSeconds <= SEVEN_DAYS_SECONDS) {
            return UrgencyBand.MEDIUM;
        }
        return UrgencyBand.LOW;
    }

    private RecommendationReason reasonFor(UrgencyBand urgency) {
        return switch (urgency) {
            case OVERDUE -> RecommendationReason.OVERDUE;
            case CRITICAL -> RecommendationReason.DUE_WITHIN_24_HOURS;
            case HIGH, MEDIUM -> RecommendationReason.DUE_SOON;
            case LOW -> RecommendationReason.UPCOMING;
        };
    }

    private StartGuidance guidanceFor(UrgencyBand urgency) {
        return switch (urgency) {
            case OVERDUE, CRITICAL -> StartGuidance.START_NOW;
            case HIGH -> StartGuidance.START_TODAY;
            case MEDIUM -> StartGuidance.START_SOON;
            case LOW -> StartGuidance.CAN_WAIT;
        };
    }
}
