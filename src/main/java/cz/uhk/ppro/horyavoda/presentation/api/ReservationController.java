package cz.uhk.ppro.horyavoda.presentation.api;

import cz.uhk.ppro.horyavoda.application.service.ReservationService;
import cz.uhk.ppro.horyavoda.presentation.dto.ReservationCreateRequest;
import cz.uhk.ppro.horyavoda.presentation.dto.ReservationPickupRequest;
import cz.uhk.ppro.horyavoda.presentation.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Rezervace typu (vyžaduje přihlášení). Rezervace se nemažou, jen ruší nebo propadají. */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final Clock clock;

    public ReservationController(ReservationService reservationService, Clock clock) {
        this.reservationService = reservationService;
        this.clock = clock;
    }

    @GetMapping
    public List<ReservationResponse> listReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        return reservationService.listReservations().stream()
                .map(reservation -> ReservationResponse.from(reservation, now)).toList();
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse getReservation(@PathVariable long reservationId) {
        return ReservationResponse.from(reservationService.getReservation(reservationId), LocalDateTime.now(clock));
    }

    /** Rezervace typu; nezbývá-li v termínu dost volných kusů, vrací 409. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(@Valid @RequestBody ReservationCreateRequest request) {
        return ReservationResponse.from(reservationService.createReservation(
                request.customerId(), request.categoryId(), request.size(), request.quantity(),
                request.plannedStart(), request.plannedEnd()), LocalDateTime.now(clock));
    }

    /** Výdej rezervace: obsluha přiřadí konkrétní kusy, vznikne výpůjčka. */
    @PostMapping("/{reservationId}/pickup")
    public ReservationResponse pickUp(@PathVariable long reservationId, @Valid @RequestBody ReservationPickupRequest request) {
        return ReservationResponse.from(
                reservationService.pickUp(reservationId, request.equipmentItemIds()), LocalDateTime.now(clock));
    }

    @PostMapping("/{reservationId}/cancel")
    public ReservationResponse cancel(@PathVariable long reservationId) {
        return ReservationResponse.from(reservationService.cancel(reservationId), LocalDateTime.now(clock));
    }
}
