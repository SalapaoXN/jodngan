package com.jodngan.scheduling.domain;

public enum RecommendationReason {
    OVERDUE("เลยกำหนดส่งแล้ว"),
    DUE_WITHIN_24_HOURS("ใกล้กำหนดส่งมาก"),
    DUE_SOON("กำหนดส่งใกล้เข้ามา"),
    UPCOMING("เป็นงานถัดไปตามกำหนดส่ง");

    private final String message;

    RecommendationReason(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
