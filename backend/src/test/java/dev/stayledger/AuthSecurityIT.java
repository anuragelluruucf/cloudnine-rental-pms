package dev.stayledger;

import dev.stayledger.domain.Role;
import dev.stayledger.domain.UserAccount;
import dev.stayledger.repo.UserAccountRepository;
import dev.stayledger.support.ApiTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserAccountRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void protectedRoutesNeedATokenAndRolesAreEnforced() throws Exception {
        mvc.perform(get("/api/properties"))
                .andExpect(status().isUnauthorized());

        String email = "host-" + UUID.randomUUID() + "@cloudnine.test";
        String token = ApiTests.register(mvc, email);

        mvc.perform(get("/api/auth/me").header("Authorization", ApiTests.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("MANAGER"));

        mvc.perform(get("/api/properties").header("Authorization", ApiTests.bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/users").header("Authorization", ApiTests.bearer(token)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/properties").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        String adminEmail = "admin-" + UUID.randomUUID() + "@cloudnine.test";
        UserAccount admin = new UserAccount();
        admin.setName("Admin");
        admin.setEmail(adminEmail);
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("cloudnine-demo"));
        users.save(admin);

        String adminToken = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"cloudnine-demo"}
                                """.formatted(adminEmail)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String tokenValue = com.jayway.jsonpath.JsonPath.read(adminToken, "$.token");

        mvc.perform(get("/api/admin/users").header("Authorization", ApiTests.bearer(tokenValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '%s')]".formatted(adminEmail)).exists());
    }
}
