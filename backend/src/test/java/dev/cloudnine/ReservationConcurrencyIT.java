package dev.cloudnine;

import dev.cloudnine.domain.Role;
import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.repo.UserAccountRepository;
import dev.cloudnine.service.PropertyService;
import dev.cloudnine.service.ReservationService;
import dev.cloudnine.support.ConcurrentBookingHarness;
import dev.cloudnine.web.Dtos.PropertyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@SpringBootTest
class ReservationConcurrencyIT {

    @Autowired
    ReservationService reservations;

    @Autowired
    PropertyService properties;

    @Autowired
    UserAccountRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void tenConcurrentBookingsProduceOneStay() throws Exception {
        UserAccount host = new UserAccount();
        host.setName("Race Host");
        host.setEmail("race-" + UUID.randomUUID() + "@cloudnine.test");
        host.setRole(Role.MANAGER);
        host.setPasswordHash(passwordEncoder.encode("cloudnine-demo"));
        users.saveAndFlush(host);

        var property = properties.create(host, new PropertyRequest(
                "Race Cabin " + UUID.randomUUID(),
                "1 Lock Lane",
                "Used by the concurrency test.",
                null, null, null, null, null, null, null
        ));

        ConcurrentBookingHarness.assertOneWinner(reservations, host, property.id());
    }
}
