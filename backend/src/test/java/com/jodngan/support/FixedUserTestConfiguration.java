package com.jodngan.support;

import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.jodngan.identity.application.AuthenticatedUser;
import com.jodngan.identity.application.CurrentUserProvider;

@TestConfiguration(proxyBeanMethods = false)
public class FixedUserTestConfiguration {

    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Bean
    @Primary
    CurrentUserProvider fixedCurrentUserProvider() {
        return () -> new AuthenticatedUser(USER_ID, "test@example.com", "Asia/Bangkok");
    }
}
