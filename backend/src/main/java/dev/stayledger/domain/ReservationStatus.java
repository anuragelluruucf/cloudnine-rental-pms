package dev.stayledger.domain;

public enum ReservationStatus {
    CONFIRMED,
    BLOCKED,
    CANCELLED,
    CONFLICT;

    public boolean occupiesNights() {
        return this == CONFIRMED || this == BLOCKED;
    }
}
