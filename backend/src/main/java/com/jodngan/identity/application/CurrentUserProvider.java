package com.jodngan.identity.application;

public interface CurrentUserProvider {
    AuthenticatedUser requireCurrentUser();
}
