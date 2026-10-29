package com.jodngan.identity.application;

import java.util.Optional;

import com.jodngan.identity.domain.UserAccount;

public interface UserRepository {
    UserAccount save(UserAccount user);
    Optional<UserAccount> findByEmail(String normalizedEmail);
}
