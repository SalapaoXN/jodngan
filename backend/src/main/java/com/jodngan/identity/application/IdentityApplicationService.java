package com.jodngan.identity.application;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jodngan.identity.domain.UserAccount;

@Service
public class IdentityApplicationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public IdentityApplicationService(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Transactional
    public UserData register(String email, String password, String timeZone) {
        String normalizedEmail = UserAccount.normalizeEmail(email);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException();
        }
        validatePassword(password);
        validateTimeZone(timeZone);

        Instant now = clock.instant();
        UserAccount user = UserAccount.create(
                normalizedEmail,
                passwordHasher.encode(password),
                timeZone,
                now);
        return UserData.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserData authenticate(String email, String password) {
        String normalizedEmail = UserAccount.normalizeEmail(email);
        UserAccount user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        if (password == null || !passwordHasher.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return UserData.from(user);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("password must contain at least 8 characters");
        }
    }

    private static void validateTimeZone(String timeZone) {
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException | NullPointerException exception) {
            throw new InvalidTimeZoneException();
        }
    }
}
