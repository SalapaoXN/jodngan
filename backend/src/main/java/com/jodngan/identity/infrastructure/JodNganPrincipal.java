package com.jodngan.identity.infrastructure;

import java.io.Serializable;
import java.util.UUID;

public record JodNganPrincipal(UUID userId, String email, String timeZone) implements Serializable {
}
