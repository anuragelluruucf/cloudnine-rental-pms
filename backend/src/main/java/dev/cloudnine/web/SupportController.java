package dev.cloudnine.web;

import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.repo.UserAccountRepository;
import dev.cloudnine.service.AuthService;
import dev.cloudnine.web.Dtos.UserResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
public class SupportController {

    private final UserAccountRepository users;

    public SupportController(UserAccountRepository users) {
        this.users = users;
    }

    @GetMapping("/api/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping(value = "/api/samples/airbnb-november.ics", produces = "text/calendar")
    ResponseEntity<String> airbnbSample() throws Exception {
        String body = new ClassPathResource("samples/airbnb-november.ics").getContentAsString(StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/calendar")).body(body);
    }

    @GetMapping("/api/admin/users")
    List<UserResponse> users() {
        return users.findAll().stream().map(AuthService::toResponse).toList();
    }
}
