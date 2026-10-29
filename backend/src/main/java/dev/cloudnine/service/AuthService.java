package dev.cloudnine.service;

import dev.cloudnine.domain.Role;
import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.repo.UserAccountRepository;
import dev.cloudnine.web.ApiException;
import dev.cloudnine.web.Dtos.AuthResponse;
import dev.cloudnine.web.Dtos.LoginRequest;
import dev.cloudnine.web.Dtos.RegisterRequest;
import dev.cloudnine.web.Dtos.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with that email already exists.");
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.MANAGER);
        users.save(user);
        return new AuthResponse(jwtService.issue(user), toResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> ApiException.unauthorized("Email or password is incorrect."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Email or password is incorrect.");
        }
        return new AuthResponse(jwtService.issue(user), toResponse(user));
    }

    public static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
