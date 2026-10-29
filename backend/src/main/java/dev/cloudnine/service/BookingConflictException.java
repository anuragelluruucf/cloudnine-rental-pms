package dev.cloudnine.service;

public class BookingConflictException extends RuntimeException {

    public static final String MESSAGE =
            "Those dates overlap an existing stay. Check-in counts and check-out does not, so a guest can arrive the same day another guest leaves.";

    public BookingConflictException() {
        super(MESSAGE);
    }
}
