package com.jodngan.identity.infrastructure;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.jodngan.identity.application.EmailAlreadyExistsException;
import com.jodngan.identity.application.UserRepository;
import com.jodngan.identity.domain.UserAccount;

@Repository
public class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserRepository repository;

    public UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserAccount save(UserAccount user) {
        try {
            return repository.saveAndFlush(UserEntity.from(user)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }
    }

    @Override
    public Optional<UserAccount> findByEmail(String normalizedEmail) {
        return repository.findByEmail(normalizedEmail).map(UserEntity::toDomain);
    }
}
