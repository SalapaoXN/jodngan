package com.jodngan.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskStatus;

class TaskLifecycleTests {

    private static final Instant CREATED_AT = Instant.parse("2026-10-29T00:00:00Z");
    private static final Instant DUE_AT = Instant.parse("2026-10-30T00:00:00Z");

    @Test
    void completeSetsCompletedStateAndTimestamp() {
        Task active = task(TaskStatus.ACTIVE, null, CREATED_AT);
        Instant completedAt = CREATED_AT.plusSeconds(300);

        Task completed = active.complete(completedAt);

        assertEquals(TaskStatus.COMPLETED, completed.status());
        assertEquals(completedAt, completed.completedAt());
        assertEquals(completedAt, completed.updatedAt());
        assertFalse(completed.deadlineProgressAt(DUE_AT.plusSeconds(60)).overdue());
    }

    @Test
    void completeAndReopenAreIdempotent() {
        Instant completedAt = CREATED_AT.plusSeconds(300);
        Task completed = task(TaskStatus.COMPLETED, completedAt, completedAt);
        assertSame(completed, completed.complete(completedAt.plusSeconds(60)));

        Task reopened = completed.reopen(completedAt.plusSeconds(120));
        assertEquals(TaskStatus.ACTIVE, reopened.status());
        assertNull(reopened.completedAt());
        assertEquals(completedAt.plusSeconds(120), reopened.updatedAt());
        assertSame(reopened, reopened.reopen(completedAt.plusSeconds(180)));
    }

    @Test
    void editPreservesLifecycleState() {
        Instant completedAt = CREATED_AT.plusSeconds(300);
        Task completed = task(TaskStatus.COMPLETED, completedAt, completedAt);
        Instant editedAt = completedAt.plusSeconds(120);

        Task edited = completed.edit("  Renamed task  ", DUE_AT.plusSeconds(3_600), editedAt);

        assertEquals("Renamed task", edited.title());
        assertEquals(DUE_AT.plusSeconds(3_600), edited.dueAt());
        assertEquals(TaskStatus.COMPLETED, edited.status());
        assertEquals(completedAt, edited.completedAt());
        assertEquals(editedAt, edited.updatedAt());
    }

    private Task task(TaskStatus status, Instant completedAt, Instant updatedAt) {
        return new Task(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Lifecycle task",
                DUE_AT,
                status,
                completedAt,
                CREATED_AT,
                updatedAt);
    }
}
