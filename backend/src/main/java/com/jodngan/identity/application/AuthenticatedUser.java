package com.jodngan.identity.application;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, String timeZone) {
}
