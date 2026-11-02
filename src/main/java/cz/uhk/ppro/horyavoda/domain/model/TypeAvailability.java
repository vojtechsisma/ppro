package cz.uhk.ppro.horyavoda.domain.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kapacita jednoho typu (kategorie + velikost) v termínu [from, to).
 * Volné = půjčovatelné kusy typu − kusy obsazené výpůjčkami − kusy držené rezervacemi typu.
 * Výpočet je konzervativní: kus obsazený kdykoli v termínu se počítá jako obsazený po celý termín.
 */
public final class TypeAvailability {

    private TypeAvailability() {
    }

    /**
     * @param itemsOfType    všechny kusy dané kategorie a velikosti
     * @param bookings       položky výpůjček těchto kusů
     * @param reservations   rezervace téhož typu (kromě té, která se právě vydává)
     * @param takenItemIds   kusy, které právě přidává nová výpůjčka
     */
    public static long freeCount(
            Collection<EquipmentItem> itemsOfType,
            Collection<RentalItem> bookings,
            Collection<Reservation> reservations,
            Set<Long> takenItemIds,
            LocalDateTime from,
            LocalDateTime to,
            LocalDateTime now
    ) {
        Set<Long> blocked = bookings.stream()
                .filter(booking -> booking.blocksEquipmentBetween(from, to, now))
                .map(booking -> booking.getEquipmentItem().getId())
                .collect(Collectors.toSet());
        long usable = itemsOfType.stream()
                .filter(EquipmentItem::isUsableForRental)
                .filter(item -> !blocked.contains(item.getId()) && !takenItemIds.contains(item.getId()))
                .count();
        long reserved = reservations.stream()
                .filter(reservation -> reservation.holdsCapacityAt(now) && reservation.overlaps(from, to))
                .mapToLong(Reservation::getQuantity)
                .sum();
        return usable - reserved;
    }
}
