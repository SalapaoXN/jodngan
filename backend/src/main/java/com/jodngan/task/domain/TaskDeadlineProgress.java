package com.jodngan.task.domain;

public record TaskDeadlineProgress(long remainingSeconds, boolean overdue) {
}
