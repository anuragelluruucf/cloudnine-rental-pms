package dev.stayledger.service;

import dev.stayledger.domain.Property;
import dev.stayledger.domain.Reservation;
import dev.stayledger.domain.ReservationStatus;
import dev.stayledger.domain.UserAccount;
import dev.stayledger.repo.ReservationRepository;
import dev.stayledger.web.Dtos.DashboardResponse;
import dev.stayledger.web.Dtos.Occupancy;
import dev.stayledger.web.Dtos.StaySnippet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DashboardService {

    private static final List<ReservationStatus> OCCUPYING =
            List.of(ReservationStatus.CONFIRMED, ReservationStatus.BLOCKED);

    private final PropertyService properties;
    private final ReservationRepository reservations;

    public DashboardService(PropertyService properties, ReservationRepository reservations) {
        this.properties = properties;
        this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UserAccount actor) {
        List<Property> owned = properties.accessible(actor);
        List<UUID> ids = owned.stream().map(Property::getId).toList();
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(14);
        LocalDate occupancyEnd = today.plusDays(30);

        List<Reservation> window = ids.isEmpty()
                ? List.of()
                : reservations.findForPropertiesOverlapping(ids, OCCUPYING, today, occupancyEnd);

        List<StaySnippet> checkIns = window.stream()
                .filter(stay -> stay.getStatus() == ReservationStatus.CONFIRMED)
                .filter(stay -> !stay.getCheckIn().isBefore(today) && stay.getCheckIn().isBefore(horizon))
                .sorted(Comparator.comparing(Reservation::getCheckIn))
                .map(DashboardService::snippet)
                .toList();

        List<StaySnippet> checkOuts = window.stream()
                .filter(stay -> stay.getStatus() == ReservationStatus.CONFIRMED)
                .filter(stay -> !stay.getCheckOut().isBefore(today) && stay.getCheckOut().isBefore(horizon))
                .sorted(Comparator.comparing(Reservation::getCheckOut))
                .map(DashboardService::snippet)
                .toList();

        int booked = 0;
        for (UUID propertyId : ids) {
            Set<LocalDate> nights = new HashSet<>();
            for (Reservation stay : window) {
                if (!stay.getProperty().getId().equals(propertyId)) {
                    continue;
                }
                LocalDate cursor = stay.getCheckIn().isBefore(today) ? today : stay.getCheckIn();
                LocalDate end = stay.getCheckOut().isAfter(occupancyEnd) ? occupancyEnd : stay.getCheckOut();
                while (cursor.isBefore(end)) {
                    nights.add(cursor);
                    cursor = cursor.plusDays(1);
                }
            }
            booked += nights.size();
        }

        Occupancy occupancy = new Occupancy(today, occupancyEnd, booked, ids.size() * 30, ids.size());
        return new DashboardResponse(checkIns, checkOuts, occupancy);
    }

    private static StaySnippet snippet(Reservation stay) {
        return new StaySnippet(
                stay.getId(),
                stay.getProperty().getId(),
                stay.getProperty().getName(),
                stay.getGuestName(),
                stay.getCheckIn(),
                stay.getCheckOut(),
                stay.getChannel(),
                stay.getStatus()
        );
    }
}
