package dev.stayledger.ical;

import java.time.LocalDate;

public record ParsedEvent(
        String uid,
        LocalDate checkIn,
        LocalDate checkOut,
        String summary,
        String description,
        boolean cancelled
) {
}
