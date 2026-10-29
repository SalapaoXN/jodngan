package com.jodngan.scheduling.domain;

public enum PlanningImportance {
    LOW(-5, 0),
    NORMAL(0, 1),
    HIGH(10, 2);

    private final int adjustment;
    private final int rank;

    PlanningImportance(int adjustment, int rank) {
        this.adjustment = adjustment;
        this.rank = rank;
    }

    int adjustment() {
        return adjustment;
    }

    int rank() {
        return rank;
    }
}
