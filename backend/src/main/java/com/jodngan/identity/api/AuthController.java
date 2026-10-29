package com.jodngan.identity.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jodngan.identity.application.CurrentUserProvider;
import com.jodngan.identity.application.IdentityApplicationService;
import com.jodngan.identity.application.UserData;
import com.jodngan.identity.infrastructure.SessionAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IdentityApplicationService identityApplicationService;
    private final CurrentUserProvider currentUserProvider;
    private final SessionAuthenticationService sessionAuthenticationService;

    public AuthController(
            IdentityApplicationService identityApplicationService,
            CurrentUserProvider currentUserProvider,
            SessionAuthenticationService sessionAuthenticationService) {
        this.identityApplicationService = identityApplicationService;
        this.currentUserProvider = currentUserProvider;
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        UserData user = identityApplicationService.register(
                request.email(),
                request.password(),
                request.timeZone());
        sessionAuthenticationService.establish(user, servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(user));
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        UserData user = identityApplicationService.authenticate(request.email(), request.password());
        sessionAuthenticationService.establish(user, servletRequest);
        return AuthResponse.from(user);
    }

    @GetMapping("/me")
    public AuthResponse me() {
        return AuthResponse.from(currentUserProvider.requireCurrentUser());
    }
}
