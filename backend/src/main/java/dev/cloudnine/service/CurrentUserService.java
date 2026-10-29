package dev.cloudnine.service;

import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.repo.UserAccountRepository;
import dev.cloudnine.web.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    public UserAccount require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw ApiException.unauthorized("Sign in required");
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            throw ApiException.unauthorized("Sign in required");
        }
        UUID id;
        try {
            id = UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            throw ApiException.unauthorized("Sign in required");
        }
        return users.findById(id).orElseThrow(() -> ApiException.unauthorized("Sign in required"));
    }
}
