package com.jodngan.scheduling.domain;

public record RiskAssessment(
        RiskState state,
        RiskReason reasonCode) {

    public String message() {
        return reasonCode == null ? null : reasonCode.message();
    }
}
