package dev.stayledger.web;

import dev.stayledger.domain.ChannelType;
import dev.stayledger.domain.MessageStatus;
import dev.stayledger.domain.ReservationStatus;
import dev.stayledger.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class Dtos {

    private Dtos() {
    }

    public record ApiError(String error, String message) {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record UserResponse(UUID id, String email, String name, Role role) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record PropertyRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 500) String address,
            @Size(max = 4000) String description,
            @Size(max = 4000) String houseRules,
            @Size(max = 4000) String amenities,
            @Size(max = 4000) String checkInInfo,
            @Size(max = 4000) String checkOutInfo,
            @Size(max = 255) String wifiNetwork,
            @Size(max = 255) String wifiPassword,
            @Size(max = 4000) String faqNotes
    ) {
    }

    public record PropertyResponse(
            UUID id,
            String name,
            String address,
            String description,
            String houseRules,
            String amenities,
            String checkInInfo,
            String checkOutInfo,
            String wifiNetwork,
            String wifiPassword,
            String faqNotes,
            UUID exportToken,
            String icalPath,
            Instant updatedAt
    ) {
    }

    public record ReservationRequest(
            @Size(max = 255) String guestName,
            @Size(max = 255) String guestEmail,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @NotNull ChannelType channel,
            @Size(max = 4000) String notes,
            boolean block
    ) {
    }

    public record ReservationResponse(
            UUID id,
            UUID propertyId,
            String propertyName,
            UUID channelSourceId,
            ChannelType channel,
            String guestName,
            String guestEmail,
            LocalDate checkIn,
            LocalDate checkOut,
            ReservationStatus status,
            String externalUid,
            boolean conflict,
            String notes
    ) {
    }

    public record ChannelRequest(
            @NotNull ChannelType channel,
            @Size(max = 1000) String importUrl
    ) {
    }

    public record ChannelResponse(
            UUID id,
            UUID propertyId,
            ChannelType channel,
            String importUrl,
            Instant lastSyncedAt,
            String lastSyncMessage,
            boolean enabled
    ) {
    }

    public record IcsImportRequest(@NotBlank String ics) {
    }

    public record ImportReport(int created, int updated, int conflicts, int cancelled, String message) {
    }

    public record AskRequest(
            @NotBlank @Size(max = 255) String guestName,
            @NotBlank @Size(max = 4000) String body
    ) {
    }

    public record DraftRequest(@NotBlank @Size(max = 8000) String reply) {
    }

    public record MessageResponse(
            UUID id,
            UUID propertyId,
            String propertyName,
            String guestName,
            String body,
            String draftReply,
            String sentReply,
            List<String> evidence,
            MessageStatus status,
            Instant createdAt,
            Instant sentAt
    ) {
    }

    public record StaySnippet(
            UUID reservationId,
            UUID propertyId,
            String propertyName,
            String guestName,
            LocalDate checkIn,
            LocalDate checkOut,
            ChannelType channel,
            ReservationStatus status
    ) {
    }

    public record Occupancy(LocalDate from, LocalDate to, int bookedNights, int totalNights, int propertyCount) {
    }

    public record DashboardResponse(
            List<StaySnippet> upcomingCheckIns,
            List<StaySnippet> upcomingCheckOuts,
            Occupancy occupancy
    ) {
    }
}
