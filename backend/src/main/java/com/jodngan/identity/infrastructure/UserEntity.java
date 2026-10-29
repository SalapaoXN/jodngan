package com.jodngan.identity.infrastructure;

import java.time.Instant;
import java.util.UUID;

import com.jodngan.identity.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    private UserEntity(UserAccount user) {
        id = user.id();
        email = user.email();
        passwordHash = user.passwordHash();
        timeZone = user.timeZone();
        createdAt = user.createdAt();
        updatedAt = user.updatedAt();
    }

    static UserEntity from(UserAccount user) {
        return new UserEntity(user);
    }

    UserAccount toDomain() {
        return new UserAccount(id, email, passwordHash, timeZone, createdAt, updatedAt);
    }
}
