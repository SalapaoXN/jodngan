package com.jodngan.identity.api;

import com.jodngan.identity.application.AuthenticatedUser;
import com.jodngan.identity.application.UserData;

public record AuthResponse(UserResponse user) {
    public static AuthResponse from(UserData user) {
        return new AuthResponse(UserResponse.from(user));
    }

    public static AuthResponse from(AuthenticatedUser user) {
        return new AuthResponse(UserResponse.from(user));
    }
}
