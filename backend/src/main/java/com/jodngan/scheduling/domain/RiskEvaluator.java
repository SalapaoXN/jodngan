package com.jodngan.scheduling.domain;

public final class RiskEvaluator {

    private RiskEvaluator() {
    }

    public static RiskAssessment assess(Integer estimatedMinutes, long remainingSeconds, boolean overdue) {
        if (overdue) {
            return new RiskAssessment(RiskState.AT_RISK, RiskReason.OVERDUE);
        }
        if (estimatedMinutes == null) {
            return new RiskAssessment(RiskState.INSUFFICIENT_DATA, RiskReason.EFFORT_UNKNOWN);
        }

        double remainingMinutes = Math.max(0.0, remainingSeconds / 60.0);
        if (estimatedMinutes >= remainingMinutes) {
            return new RiskAssessment(RiskState.AT_RISK, RiskReason.INSUFFICIENT_CLOCK_TIME);
        }

        return new RiskAssessment(RiskState.NO_CONFIRMED_RISK, null);
    }
}
