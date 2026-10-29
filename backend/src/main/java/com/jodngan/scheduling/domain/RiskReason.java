package com.jodngan.scheduling.domain;

public enum RiskReason {
    OVERDUE("เลยกำหนดส่งแล้ว"),
    EFFORT_UNKNOWN("เพิ่มเวลาที่คาดว่าจะใช้เพื่อประเมินความเสี่ยงได้ดีขึ้น"),
    INSUFFICIENT_CLOCK_TIME("เวลาที่เหลือน้อยกว่างานที่ประเมินไว้");

    private final String message;

    RiskReason(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
