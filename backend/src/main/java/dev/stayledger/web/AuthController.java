package dev.stayledger.web;

import dev.stayledger.service.AuthService;
import dev.stayledger.service.CurrentUserService;
import dev.stayledger.web.Dtos.AuthResponse;
import dev.stayledger.web.Dtos.LoginRequest;
import dev.stayledger.web.Dtos.RegisterRequest;
import dev.stayledger.web.Dtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final CurrentUserService currentUser;

    public AuthController(AuthService auth, CurrentUserService currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    UserResponse me() {
        return AuthService.toResponse(currentUser.require());
    }
}
