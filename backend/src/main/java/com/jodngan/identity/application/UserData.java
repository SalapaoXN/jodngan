package com.jodngan.identity.application;

import java.util.UUID;

import com.jodngan.identity.domain.UserAccount;

public record UserData(UUID id, String email, String timeZone) {
    public static UserData from(UserAccount user) {
        return new UserData(user.id(), user.email(), user.timeZone());
    }
}
