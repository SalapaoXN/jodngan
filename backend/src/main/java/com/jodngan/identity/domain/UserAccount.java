package com.jodngan.identity.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String email,
        String passwordHash,
        String timeZone,
        Instant createdAt,
        Instant updatedAt) {

    public UserAccount {
        Objects.requireNonNull(id, "id is required");
        email = normalizeEmail(email);
        if (email.isEmpty() || email.length() > 320) {
            throw new IllegalArgumentException("email is invalid");
        }
        passwordHash = Objects.requireNonNull(passwordHash, "passwordHash is required");
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        timeZone = Objects.requireNonNull(timeZone, "timeZone is required").strip();
        ZoneId.of(timeZone);
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static UserAccount create(String email, String passwordHash, String timeZone, Instant now) {
        return new UserAccount(UUID.randomUUID(), email, passwordHash, timeZone, now, now);
    }

    public static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email is required").strip().toLowerCase(Locale.ROOT);
    }
}
