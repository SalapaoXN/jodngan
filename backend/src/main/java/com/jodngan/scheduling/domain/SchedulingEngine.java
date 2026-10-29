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
            .comparingInt(RankedTask::recommendationScore)
            .reversed()
            .thenComparing(task -> task.task().dueAt())
            .thenComparing((left, right) -> Integer.compare(
                    importanceRank(right.task()),
                    importanceRank(left.task())))
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
        Double effortPressureRatio = effortPressureRatioFor(task, remainingSeconds, overdue);
        int recommendationScore = urgency.baseScore()
                + effortPressureBonus(effortPressureRatio)
                + effectiveImportance(task).adjustment();
        RiskAssessment risk = RiskEvaluator.assess(task.estimatedMinutes(), remainingSeconds, overdue);
        RecommendationReason reason = reasonFor(task, urgency, effortPressureRatio, risk, overdue);
        StartGuidance startGuidance = guidanceFor(task, urgency, effortPressureRatio, risk);
        return new RankedTask(
                task,
                urgency,
                recommendationScore,
                effortPressureRatio,
                reason,
                startGuidance,
                risk,
                remainingSeconds,
                overdue);
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

    private RecommendationReason reasonFor(
            SchedulableTask task,
            UrgencyBand urgency,
            Double effortPressureRatio,
            RiskAssessment risk,
            boolean overdue) {
        if (overdue) {
            return RecommendationReason.OVERDUE;
        }
        if (risk.reasonCode() == RiskReason.INSUFFICIENT_CLOCK_TIME) {
            return RecommendationReason.INSUFFICIENT_CLOCK_TIME;
        }
        if (urgency == UrgencyBand.CRITICAL) {
            return RecommendationReason.DUE_WITHIN_24_HOURS;
        }
        if (effortPressureRatio != null && effortPressureRatio >= 0.25) {
            return RecommendationReason.HIGH_EFFORT_PRESSURE;
        }
        if (task.importance() == PlanningImportance.HIGH) {
            return RecommendationReason.HIGH_IMPORTANCE;
        }
        if (urgency == UrgencyBand.HIGH || urgency == UrgencyBand.MEDIUM) {
            return RecommendationReason.DUE_SOON;
        }
        return RecommendationReason.UPCOMING;
    }

    private StartGuidance guidanceFor(
            SchedulableTask task,
            UrgencyBand urgency,
            Double effortPressureRatio,
            RiskAssessment risk) {
        if (urgency == UrgencyBand.OVERDUE
                || urgency == UrgencyBand.CRITICAL
                || risk.state() == RiskState.AT_RISK) {
            return StartGuidance.START_NOW;
        }
        if (urgency == UrgencyBand.HIGH
                || (effortPressureRatio != null && effortPressureRatio >= 0.25)) {
            return StartGuidance.START_TODAY;
        }
        if (urgency == UrgencyBand.MEDIUM || task.importance() == PlanningImportance.HIGH) {
            return StartGuidance.START_SOON;
        }
        return StartGuidance.CAN_WAIT;
    }

    private Double effortPressureRatioFor(SchedulableTask task, long remainingSeconds, boolean overdue) {
        if (task.estimatedMinutes() == null || overdue || remainingSeconds <= 0) {
            return null;
        }
        return task.estimatedMinutes() / (remainingSeconds / 60.0);
    }

    private int effortPressureBonus(Double pressureRatio) {
        if (pressureRatio == null) {
            return 0;
        }
        if (pressureRatio >= 0.50) {
            return 30;
        }
        if (pressureRatio >= 0.25) {
            return 20;
        }
        if (pressureRatio >= 0.10) {
            return 10;
        }
        return 0;
    }

    private static PlanningImportance effectiveImportance(SchedulableTask task) {
        return task.importance() == null ? PlanningImportance.NORMAL : task.importance();
    }

    private static int importanceRank(SchedulableTask task) {
        return effectiveImportance(task).rank();
    }
}
