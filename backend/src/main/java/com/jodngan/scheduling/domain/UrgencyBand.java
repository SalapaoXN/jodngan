package com.jodngan.scheduling.domain;

public enum UrgencyBand {
    OVERDUE(100),
    CRITICAL(80),
    HIGH(60),
    MEDIUM(40),
    LOW(20);

    private final int baseScore;

    UrgencyBand(int baseScore) {
        this.baseScore = baseScore;
    }

    int baseScore() {
        return baseScore;
    }
}
