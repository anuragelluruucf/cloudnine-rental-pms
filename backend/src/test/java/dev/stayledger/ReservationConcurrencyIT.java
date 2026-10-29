package dev.stayledger;

import dev.stayledger.domain.Role;
import dev.stayledger.domain.UserAccount;
import dev.stayledger.repo.UserAccountRepository;
import dev.stayledger.service.PropertyService;
import dev.stayledger.service.ReservationService;
import dev.stayledger.support.ConcurrentBookingHarness;
import dev.stayledger.web.Dtos.PropertyRequest;
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
