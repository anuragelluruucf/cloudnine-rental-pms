package dev.stayledger;

import dev.stayledger.domain.Role;
import dev.stayledger.domain.UserAccount;
import dev.stayledger.repo.UserAccountRepository;
import dev.stayledger.service.PropertyService;
import dev.stayledger.service.ReservationService;
import dev.stayledger.support.ConcurrentBookingHarness;
import dev.stayledger.support.DockerGate;
import dev.stayledger.web.Dtos.PropertyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@EnabledIf("dev.stayledger.support.DockerGate#available")
@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers
class PostgresConcurrencyIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/postgres");
    }

    @Autowired
    ReservationService reservations;

    @Autowired
    PropertyService properties;

    @Autowired
    UserAccountRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void tenConcurrentBookingsProduceOneStayOnPostgres() throws Exception {
        if (!DockerGate.available()) {
            return;
        }
        UserAccount host = new UserAccount();
        host.setName("Postgres Host");
        host.setEmail("pg-" + UUID.randomUUID() + "@cloudnine.test");
        host.setRole(Role.MANAGER);
        host.setPasswordHash(passwordEncoder.encode("cloudnine-demo"));
        users.saveAndFlush(host);

        var property = properties.create(host, new PropertyRequest(
                "Postgres Cabin " + UUID.randomUUID(),
                "2 Lock Lane",
                "Postgres concurrency test.",
                null, null, null, null, null, null, null
        ));

        ConcurrentBookingHarness.assertOneWinner(reservations, host, property.id());
    }
}
