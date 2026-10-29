package dev.cloudnine.support;

import dev.cloudnine.domain.ChannelType;
import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.service.BookingConflictException;
import dev.cloudnine.service.ReservationService;
import dev.cloudnine.web.Dtos.ReservationRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ConcurrentBookingHarness {

    private ConcurrentBookingHarness() {
    }

    public static void assertOneWinner(ReservationService service, UserAccount user, UUID propertyId) throws Exception {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        LocalDate checkIn = LocalDate.of(2026, 12, 1);
        LocalDate checkOut = LocalDate.of(2026, 12, 6);

        try {
            for (int i = 0; i < threads; i++) {
                int guest = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(20, TimeUnit.SECONDS)) {
                        return "late";
                    }
                    try {
                        service.create(user, propertyId, new ReservationRequest(
                                "Guest " + guest,
                                null,
                                checkIn,
                                checkOut,
                                ChannelType.DIRECT,
                                null,
                                false
                        ));
                        return "ok";
                    } catch (BookingConflictException ex) {
                        return "conflict";
                    }
                }));
            }
            if (!ready.await(20, TimeUnit.SECONDS)) {
                throw new AssertionError("Workers did not get ready");
            }
            start.countDown();

            int wins = 0;
            int conflicts = 0;
            for (Future<String> future : futures) {
                String outcome = future.get(60, TimeUnit.SECONDS);
                if ("ok".equals(outcome)) {
                    wins++;
                } else if ("conflict".equals(outcome)) {
                    conflicts++;
                } else {
                    throw new AssertionError("Unexpected outcome: " + outcome);
                }
            }
            assertEquals(1, wins, "exactly one booking should commit");
            assertEquals(9, conflicts, "the other nine should be overlap conflicts");
        } finally {
            pool.shutdownNow();
        }
    }
}
