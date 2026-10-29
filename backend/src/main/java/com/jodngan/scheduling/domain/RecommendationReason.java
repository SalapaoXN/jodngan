package com.jodngan.scheduling.domain;

public enum RecommendationReason {
    OVERDUE("เลยกำหนดส่งแล้ว"),
    INSUFFICIENT_CLOCK_TIME("เวลาที่เหลือน้อยกว่างานที่ประเมินไว้"),
    DUE_WITHIN_24_HOURS("ใกล้กำหนดส่งมาก"),
    HIGH_EFFORT_PRESSURE("งานค่อนข้างใหญ่เมื่อเทียบกับเวลาที่เหลือ"),
    HIGH_IMPORTANCE("ตั้งความสำคัญไว้สูง"),
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
