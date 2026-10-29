package com.jodngan.identity.api;

import java.util.UUID;

import com.jodngan.identity.application.AuthenticatedUser;
import com.jodngan.identity.application.UserData;

public record UserResponse(UUID id, String email, String timeZone) {
    public static UserResponse from(UserData user) {
        return new UserResponse(user.id(), user.email(), user.timeZone());
    }

    public static UserResponse from(AuthenticatedUser user) {
        return new UserResponse(user.userId(), user.email(), user.timeZone());
    }
}
