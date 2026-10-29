package dev.stayledger.web;

import dev.stayledger.service.CurrentUserService;
import dev.stayledger.service.ReservationService;
import dev.stayledger.web.Dtos.ReservationRequest;
import dev.stayledger.web.Dtos.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class ReservationController {

    private final ReservationService reservations;
    private final CurrentUserService currentUser;

    public ReservationController(ReservationService reservations, CurrentUserService currentUser) {
        this.reservations = reservations;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/properties/{propertyId}/reservations")
    List<ReservationResponse> list(
            @PathVariable UUID propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reservations.list(currentUser.require(), propertyId, from, to);
    }

    @PostMapping("/api/properties/{propertyId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    ReservationResponse create(@PathVariable UUID propertyId, @Valid @RequestBody ReservationRequest request) {
        return reservations.create(currentUser.require(), propertyId, request);
    }

    @PutMapping("/api/reservations/{id}")
    ReservationResponse update(@PathVariable UUID id, @Valid @RequestBody ReservationRequest request) {
        return reservations.update(currentUser.require(), id, request);
    }

    @PostMapping("/api/reservations/{id}/cancel")
    ReservationResponse cancel(@PathVariable UUID id) {
        return reservations.cancel(currentUser.require(), id);
    }
}
