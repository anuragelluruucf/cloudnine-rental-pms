package dev.stayledger.service;

import dev.stayledger.domain.ChannelSource;
import dev.stayledger.domain.ChannelType;
import dev.stayledger.domain.Property;
import dev.stayledger.domain.Reservation;
import dev.stayledger.domain.ReservationStatus;
import dev.stayledger.domain.UserAccount;
import dev.stayledger.ical.ParsedEvent;
import dev.stayledger.repo.ChannelSourceRepository;
import dev.stayledger.repo.PropertyRepository;
import dev.stayledger.repo.ReservationRepository;
import dev.stayledger.web.ApiException;
import dev.stayledger.web.Dtos.ReservationRequest;
import dev.stayledger.web.Dtos.ReservationResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {

    static final List<ReservationStatus> OCCUPYING = List.of(ReservationStatus.CONFIRMED, ReservationStatus.BLOCKED);

    public enum ImportOutcome {
        CREATED, UPDATED, CONFLICT, CANCELLED
    }

    private final ReservationRepository reservations;
    private final PropertyRepository properties;
    private final ChannelSourceRepository channels;
    private final PropertyService propertyService;

    public ReservationService(
            ReservationRepository reservations,
            PropertyRepository properties,
            ChannelSourceRepository channels,
            PropertyService propertyService
    ) {
        this.reservations = reservations;
        this.properties = properties;
        this.channels = channels;
        this.propertyService = propertyService;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> list(UserAccount actor, UUID propertyId, LocalDate from, LocalDate to) {
        propertyService.requireReadable(actor, propertyId);
        List<Reservation> rows = (from == null || to == null)
                ? reservations.findByPropertyIdOrderByCheckInAsc(propertyId)
                : reservations.findOverlappingRange(propertyId, from, to);
        return rows.stream().map(ReservationService::toResponse).toList();
    }

    /**
     * Locks the property row, then rejects a stay whose half-open range overlaps
     * an existing confirmed stay or block. Check-in is inclusive and check-out is exclusive:
     * existing.checkIn &lt; new.checkOut AND existing.checkOut &gt; new.checkIn.
     * Inserting a reservation does not bump a version on the property, so the row lock
     * is what serializes concurrent bookings.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse create(UserAccount actor, UUID propertyId, ReservationRequest request) {
        propertyService.requireReadable(actor, propertyId);
        validateRange(request.checkIn(), request.checkOut());
        if (!request.block() && (request.guestName() == null || request.guestName().isBlank())) {
            throw ApiException.badRequest("A guest name is required unless you are blocking the dates.");
        }
        Property property = lock(propertyId);
        ensureFree(propertyId, request.checkIn(), request.checkOut(), null);

        Reservation reservation = new Reservation();
        reservation.setProperty(property);
        reservation.setChannel(request.channel() == null ? ChannelType.DIRECT : request.channel());
        reservation.setGuestName(displayName(request));
        reservation.setGuestEmail(blankToNull(request.guestEmail()));
        reservation.setCheckIn(request.checkIn());
        reservation.setCheckOut(request.checkOut());
        reservation.setNotes(blankToNull(request.notes()));
        reservation.setStatus(request.block() ? ReservationStatus.BLOCKED : ReservationStatus.CONFIRMED);
        reservation.setConflict(false);
        saveGuardingConstraint(reservation);
        return toResponse(reservation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse update(UserAccount actor, UUID reservationId, ReservationRequest request) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> ApiException.notFound("Reservation not found"));
        propertyService.requireReadable(actor, reservation.getProperty().getId());
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw ApiException.badRequest("That stay is cancelled.");
        }
        validateRange(request.checkIn(), request.checkOut());
        UUID propertyId = reservation.getProperty().getId();
        lock(propertyId);
        if (request.block() || reservation.getStatus() == ReservationStatus.BLOCKED || reservation.getStatus() == ReservationStatus.CONFIRMED) {
            ReservationStatus next = request.block() ? ReservationStatus.BLOCKED : ReservationStatus.CONFIRMED;
            if (next.occupiesNights()) {
                ensureFree(propertyId, request.checkIn(), request.checkOut(), reservation.getId());
            }
            reservation.setStatus(next);
            reservation.setConflict(false);
        }
        reservation.setChannel(request.channel());
        reservation.setGuestName(displayName(request));
        reservation.setGuestEmail(blankToNull(request.guestEmail()));
        reservation.setCheckIn(request.checkIn());
        reservation.setCheckOut(request.checkOut());
        reservation.setNotes(blankToNull(request.notes()));
        saveGuardingConstraint(reservation);
        return toResponse(reservation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse cancel(UserAccount actor, UUID reservationId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> ApiException.notFound("Reservation not found"));
        propertyService.requireReadable(actor, reservation.getProperty().getId());
        lock(reservation.getProperty().getId());
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setConflict(false);
        return toResponse(reservations.save(reservation));
    }

    /**
     * Imports one external event. Matches on (channel, UID), then on (channel, dates)
     * when a feed changes the UID between fetches. An overlap with a different stay is
     * stored as a flagged conflict and does not change the other stay.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ImportOutcome importExternal(UUID channelId, ParsedEvent event) {
        ChannelSource channel = channels.findById(channelId)
                .orElseThrow(() -> ApiException.notFound("Channel not found"));
        UUID propertyId = channel.getProperty().getId();
        lock(propertyId);

        Optional<Reservation> existing = reservations.findByChannelSourceIdAndExternalUid(channelId, event.uid());
        if (existing.isEmpty()) {
            List<Reservation> sameDates = reservations.findByChannelSourceAndDates(
                    channelId, event.checkIn(), event.checkOut());
            if (sameDates.size() == 1) {
                existing = Optional.of(sameDates.getFirst());
            }
        }

        if (existing.isPresent()) {
            return updateImported(existing.get(), channel, event);
        }

        boolean overlaps = overlaps(propertyId, event.checkIn(), event.checkOut(), null);
        Reservation created = new Reservation();
        created.setProperty(properties.getReferenceById(propertyId));
        created.setChannelSource(channel);
        created.setChannel(channel.getChannel());
        applyEvent(created, event, overlaps);
        saveGuardingConstraint(created);
        if (event.cancelled()) {
            return ImportOutcome.CANCELLED;
        }
        return overlaps ? ImportOutcome.CONFLICT : ImportOutcome.CREATED;
    }

    private ImportOutcome updateImported(Reservation reservation, ChannelSource channel, ParsedEvent event) {
        boolean overlaps = overlaps(reservation.getProperty().getId(), event.checkIn(), event.checkOut(), reservation.getId());
        reservation.setChannelSource(channel);
        reservation.setChannel(channel.getChannel());
        applyEvent(reservation, event, overlaps);
        saveGuardingConstraint(reservation);
        if (event.cancelled()) {
            return ImportOutcome.CANCELLED;
        }
        return overlaps ? ImportOutcome.CONFLICT : ImportOutcome.UPDATED;
    }

    private void applyEvent(Reservation reservation, ParsedEvent event, boolean overlaps) {
        reservation.setExternalUid(event.uid());
        reservation.setGuestName(event.summary().isBlank() ? "Imported block" : event.summary());
        reservation.setNotes(event.description().isBlank() ? null : event.description());
        reservation.setCheckIn(event.checkIn());
        reservation.setCheckOut(event.checkOut());
        if (event.cancelled()) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setConflict(false);
            return;
        }
        if (overlaps) {
            reservation.setStatus(ReservationStatus.CONFLICT);
            reservation.setConflict(true);
            return;
        }
        reservation.setStatus(ReservationStatus.BLOCKED);
        reservation.setConflict(false);
    }

    private Property lock(UUID propertyId) {
        return properties.findByIdForUpdate(propertyId)
                .orElseThrow(() -> ApiException.notFound("Property not found"));
    }

    private void ensureFree(UUID propertyId, LocalDate checkIn, LocalDate checkOut, UUID excludeId) {
        if (overlaps(propertyId, checkIn, checkOut, excludeId)) {
            throw new BookingConflictException();
        }
    }

    private boolean overlaps(UUID propertyId, LocalDate checkIn, LocalDate checkOut, UUID excludeId) {
        long count = excludeId == null
                ? reservations.countOverlap(propertyId, OCCUPYING, checkIn, checkOut)
                : reservations.countOverlapExcluding(propertyId, OCCUPYING, checkIn, checkOut, excludeId);
        return count > 0;
    }

    private void saveGuardingConstraint(Reservation reservation) {
        try {
            reservations.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            if (isOverlapConstraint(ex)) {
                throw new BookingConflictException();
            }
            throw ex;
        }
    }

    private static boolean isOverlapConstraint(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("reservations_no_overlap") || message.toLowerCase().contains("exclusion"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void validateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw ApiException.badRequest("Check-out has to be after check-in.");
        }
    }

    private static String displayName(ReservationRequest request) {
        if (request.guestName() != null && !request.guestName().isBlank()) {
            return request.guestName().trim();
        }
        return request.block() ? "Blocked" : null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getProperty().getId(),
                reservation.getProperty().getName(),
                reservation.getChannelSource() == null ? null : reservation.getChannelSource().getId(),
                reservation.getChannel(),
                reservation.getGuestName(),
                reservation.getGuestEmail(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getStatus(),
                reservation.getExternalUid(),
                reservation.isConflict(),
                reservation.getNotes()
        );
    }
}
