package cz.uhk.ppro.horyavoda.presentation.api;

import cz.uhk.ppro.horyavoda.application.service.RentalService;
import cz.uhk.ppro.horyavoda.presentation.dto.ItemReturnRequest;
import cz.uhk.ppro.horyavoda.presentation.dto.RentalCreateRequest;
import cz.uhk.ppro.horyavoda.presentation.dto.RentalResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Výpůjčky (vyžaduje přihlášení – obsahují osobní údaje zákazníků). Výpůjčky se nemažou. */
@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;
    private final Clock clock;

    public RentalController(RentalService rentalService, Clock clock) {
        this.rentalService = rentalService;
        this.clock = clock;
    }

    /** Seznam výpůjček; s {@code equipment_item_id} historie jednoho kusu. */
    @GetMapping
    public List<RentalResponse> listRentals(@RequestParam(name = "equipment_item_id", required = false) Long equipmentItemId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return rentalService.listRentals(equipmentItemId).stream().map(rental -> RentalResponse.from(rental, now)).toList();
    }

    @GetMapping("/{rentalId}")
    public RentalResponse getRental(@PathVariable long rentalId) {
        return RentalResponse.from(rentalService.getRental(rentalId), LocalDateTime.now(clock));
    }

    /** Sjednání výpůjčky; při překryvu termínu nebo kusu v servisu vrací 409. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse createRental(@Valid @RequestBody RentalCreateRequest request) {
        return RentalResponse.from(rentalService.createRental(
                request.customerId(), request.plannedStart(), request.plannedEnd(), request.equipmentItemIds(),
                request.depositAmountOrZero(), request.discountAmountOrZero()), LocalDateTime.now(clock));
    }

    /** Vrácení jednoho kusu z výpůjčky (částečné vracení). */
    @PostMapping("/{rentalId}/items/{rentalItemId}/return")
    public RentalResponse returnItem(
            @PathVariable long rentalId,
            @PathVariable long rentalItemId,
            @RequestBody(required = false) ItemReturnRequest request
    ) {
        return RentalResponse.from(rentalService.returnItem(
                rentalId, rentalItemId, request != null ? request.returnedAt() : null), LocalDateTime.now(clock));
    }
}
