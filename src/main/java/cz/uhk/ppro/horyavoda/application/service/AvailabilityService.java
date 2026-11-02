package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.domain.model.Category;
import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.Reservation;
import cz.uhk.ppro.horyavoda.domain.model.ReservationStatus;
import cz.uhk.ppro.horyavoda.domain.model.RentalItem;
import cz.uhk.ppro.horyavoda.domain.model.TypeAvailability;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.EquipmentItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.RentalItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Kapacita typu (kategorie + velikost) v termínu. Sdílí ji výpůjčky i rezervace:
 * výpůjčka nesmí sebrat kus, který drží rezervace typu, a rezervace nesmí slíbit víc, než je volné.
 * Kusy kategorie se zamykají, takže souběžné požadavky na stejný typ se seřadí za sebou.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class AvailabilityService {

    private final EquipmentItemRepository equipmentItemRepository;
    private final RentalItemRepository rentalItemRepository;
    private final ReservationRepository reservationRepository;

    public AvailabilityService(
            EquipmentItemRepository equipmentItemRepository,
            RentalItemRepository rentalItemRepository,
            ReservationRepository reservationRepository
    ) {
        this.equipmentItemRepository = equipmentItemRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Počet kusů typu, které v termínu zbývají po odečtení výpůjček a platných rezervací.
     * Záporné číslo znamená, že by rezervace typu nešly splnit.
     */
    public long freeCount(
            Category category,
            String size,
            LocalDateTime from,
            LocalDateTime to,
            LocalDateTime now,
            Long excludedReservationId,
            Set<Long> takenItemIds
    ) {
        List<EquipmentItem> itemsOfType = equipmentItemRepository.findAllByCategoryIdForUpdate(category.getId()).stream()
                .filter(item -> Reservation.sameSize(item.getSize(), size))
                .toList();
        List<RentalItem> bookings = itemsOfType.isEmpty()
                ? List.of()
                : rentalItemRepository.findAllByEquipmentItemIds(itemsOfType.stream().map(EquipmentItem::getId).toList());
        List<Reservation> reservations = reservationRepository
                .findByCategoryIdAndStatus(category.getId(), ReservationStatus.ACTIVE).stream()
                .filter(reservation -> Reservation.sameSize(reservation.getSize(), size))
                .filter(reservation -> !Objects.equals(reservation.getId(), excludedReservationId))
                .toList();
        return TypeAvailability.freeCount(itemsOfType, bookings, reservations, takenItemIds, from, to, now);
    }
}
