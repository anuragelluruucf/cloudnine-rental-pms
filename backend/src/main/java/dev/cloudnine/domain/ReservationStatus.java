package dev.cloudnine.domain;

public enum ReservationStatus {
    CONFIRMED,
    BLOCKED,
    CANCELLED,
    CONFLICT;

    public boolean occupiesNights() {
        return this == CONFIRMED || this == BLOCKED;
    }
}
