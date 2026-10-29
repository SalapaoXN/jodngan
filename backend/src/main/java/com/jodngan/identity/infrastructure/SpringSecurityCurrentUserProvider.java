package com.jodngan.identity.infrastructure;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.jodngan.identity.application.AuthenticatedUser;
import com.jodngan.identity.application.CurrentUserProvider;

@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public AuthenticatedUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof JodNganPrincipal principal)) {
            throw new IllegalStateException("Authenticated JodNgan user is required");
        }
        return new AuthenticatedUser(principal.userId(), principal.email(), principal.timeZone());
    }
}
