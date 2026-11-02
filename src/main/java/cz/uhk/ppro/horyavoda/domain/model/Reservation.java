package cz.uhk.ppro.horyavoda.domain.model;

import cz.uhk.ppro.horyavoda.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

/**
 * Rezervace typu (kategorie + velikost + počet kusů) na termín.
 * Konkrétní kusy se přiřazují až při výdeji na pultu; do té doby rezervace drží jen kapacitu.
 * Ranní rezervace drží do 10:00 s tolerancí 30 minut, pak se sama uvolní.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    /** Klient: rezervace drží „do desíti“. */
    public static final LocalTime PICKUP_HOLD_UNTIL = LocalTime.of(10, 0);
    /** Tolerance na zpoždění cestou, než rezervace propadne. */
    public static final Duration PICKUP_TOLERANCE = Duration.ofMinutes(30);
    public static final int MAX_QUANTITY = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "size", nullable = false, length = 30)
    private String size;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "planned_start", nullable = false)
    private LocalDateTime plannedStart;

    @Column(name = "planned_end", nullable = false)
    private LocalDateTime plannedEnd;

    @Column(name = "pickup_deadline", nullable = false)
    private LocalDateTime pickupDeadline;

    /** Snapshot ceny z ceníku v okamžiku rezervace – platí i pro výpůjčku, která z ní vznikne. */
    @Column(name = "unit_daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitDailyPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private Rental rental;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Reservation() {
        // vyžadováno JPA
    }

    public Reservation(
            Customer customer,
            Category category,
            String size,
            int quantity,
            LocalDateTime plannedStart,
            LocalDateTime plannedEnd,
            LocalDateTime now
    ) {
        if (!plannedEnd.isAfter(plannedStart)) {
            throw new DomainException("Konec rezervace musí být později než její začátek.");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new DomainException("Rezervovat lze 1 až " + MAX_QUANTITY + " kusů.");
        }
        this.customer = customer;
        this.category = category;
        this.size = size.strip();
        this.quantity = quantity;
        this.plannedStart = plannedStart;
        this.plannedEnd = plannedEnd;
        this.pickupDeadline = pickupDeadlineFor(plannedStart);
        if (now.isAfter(holdsUntil())) {
            throw new DomainException("Rezervace by propadla dřív, než vznikne – čas vyzvednutí už minul.");
        }
        this.unitDailyPrice = category.getBaseDailyRate();
    }

    /** Vyzvednutí do 10:00 v den začátku; začíná-li rezervace až po 10:00, tak do jejího začátku. */
    static LocalDateTime pickupDeadlineFor(LocalDateTime plannedStart) {
        LocalDateTime morningLimit = plannedStart.toLocalDate().atTime(PICKUP_HOLD_UNTIL);
        return plannedStart.isAfter(morningLimit) ? plannedStart : morningLimit;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Poslední okamžik, kdy rezervace ještě drží (čas vyzvednutí + tolerance). */
    public LocalDateTime holdsUntil() {
        return pickupDeadline.plus(PICKUP_TOLERANCE);
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return status == ReservationStatus.ACTIVE && now.isAfter(holdsUntil());
    }

    /** Zda rezervace v daném okamžiku drží kapacitu svého typu. */
    public boolean holdsCapacityAt(LocalDateTime now) {
        return status == ReservationStatus.ACTIVE && !isExpiredAt(now);
    }

    /** Stav tak, jak platí v daném okamžiku (propadlou rezervaci ukáže jako EXPIRED i před během úlohy). */
    public ReservationStatus statusAt(LocalDateTime now) {
        return isExpiredAt(now) ? ReservationStatus.EXPIRED : status;
    }

    public boolean overlaps(LocalDateTime from, LocalDateTime to) {
        return plannedStart.isBefore(to) && from.isBefore(plannedEnd);
    }

    public boolean isOfType(EquipmentItem item) {
        return item.getCategory().getId().equals(category.getId()) && sameSize(item.getSize(), size);
    }

    public static boolean sameSize(String first, String second) {
        return first.strip().toLowerCase(Locale.ROOT).equals(second.strip().toLowerCase(Locale.ROOT));
    }

    /** Ověří, že si zákazník může rezervaci teď vyzvednout. */
    public void ensurePickableAt(LocalDateTime now) {
        if (isExpiredAt(now)) {
            throw new DomainException("Rezervace č. " + id + " propadla, vyzvednout se měla do "
                    + holdsUntil().toLocalTime() + ".");
        }
        if (status != ReservationStatus.ACTIVE) {
            throw new DomainException("Rezervace č. " + id + " už není platná.");
        }
    }

    /** Výdej: z rezervace vznikla výpůjčka s konkrétními kusy. */
    public void fulfil(Rental rental) {
        this.rental = rental;
        this.status = ReservationStatus.FULFILLED;
    }

    public void expire() {
        if (status == ReservationStatus.ACTIVE) {
            status = ReservationStatus.EXPIRED;
        }
    }

    public void cancel() {
        if (status != ReservationStatus.ACTIVE) {
            throw new DomainException("Zrušit lze jen platnou rezervaci.");
        }
        status = ReservationStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Category getCategory() {
        return category;
    }

    public String getSize() {
        return size;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getPlannedStart() {
        return plannedStart;
    }

    public LocalDateTime getPlannedEnd() {
        return plannedEnd;
    }

    public LocalDateTime getPickupDeadline() {
        return pickupDeadline;
    }

    public BigDecimal getUnitDailyPrice() {
        return unitDailyPrice;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Rental getRental() {
        return rental;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
