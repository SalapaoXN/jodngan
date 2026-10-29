package com.jodngan.course.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Course(
        UUID id,
        UUID userId,
        String name,
        Instant createdAt,
        Instant updatedAt) {

    public Course {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(userId, "userId is required");
        name = Objects.requireNonNull(name, "name is required").strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.length() > 120) {
            throw new IllegalArgumentException("name must not exceed 120 characters");
        }
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static Course create(UUID userId, String name, Instant now) {
        return new Course(UUID.randomUUID(), userId, name, now, now);
    }

    public Course rename(String name, Instant now) {
        return new Course(id, userId, name, createdAt, now);
    }
}
