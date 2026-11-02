package cz.uhk.ppro.horyavoda.domain.model;

import cz.uhk.ppro.horyavoda.domain.exception.DomainException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityNotFoundException;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Výpůjčka – smlouva se zákazníkem obsahující jeden či více kusů vybavení. */
@Entity
@Table(name = "rentals")
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "planned_start", nullable = false)
    private LocalDateTime plannedStart;

    @Column(name = "planned_end", nullable = false)
    private LocalDateTime plannedEnd;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RentalStatus status = RentalStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "rental", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalItem> items = new ArrayList<>();

    protected Rental() {
        // vyžadováno JPA
    }

    public Rental(Customer customer, LocalDateTime plannedStart, LocalDateTime plannedEnd, BigDecimal depositAmount) {
        if (!plannedEnd.isAfter(plannedStart)) {
            throw new DomainException("Konec výpůjčky musí být později než její začátek.");
        }
        this.customer = customer;
        this.plannedStart = plannedStart;
        this.plannedEnd = plannedEnd;
        this.depositAmount = depositAmount;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Přidá kus do výpůjčky se snapshotem aktuálně sjednané denní ceny.
     * Kus v servisu ani vyřazený kus přidat nelze; kontrolu překryvu s jinými
     * výpůjčkami dělá aplikační služba, protože potřebuje data z úložiště.
     */
    public RentalItem addItem(EquipmentItem equipmentItem, BigDecimal unitDailyPrice) {
        equipmentItem.ensureRentable();
        boolean alreadyIncluded = items.stream()
                .anyMatch(item -> item.getEquipmentItem().getId().equals(equipmentItem.getId()));
        if (alreadyIncluded) {
            throw new DomainException("Kus " + equipmentItem.getInventoryNumber() + " je ve výpůjčce dvakrát.");
        }
        RentalItem item = new RentalItem(this, equipmentItem, unitDailyPrice);
        items.add(item);
        return item;
    }

    /** Výdej na pultu: všechny kusy výpůjčky jsou od teď venku. */
    public void handOverItems() {
        items.forEach(item -> item.getEquipmentItem().handOver());
    }

    /**
     * Vrácení jednoho kusu. Výpůjčka zůstává otevřená, dokud se nevrátí poslední kus;
     * do té doby je ve stavu RETURNED_PARTIAL.
     */
    public RentalItem returnItem(long rentalItemId, LocalDateTime returnedAt) {
        if (status == RentalStatus.CANCELLED || status == RentalStatus.COMPLETED) {
            throw new DomainException("Výpůjčka č. " + id + " je uzavřená, kus z ní nelze vrátit.");
        }
        RentalItem item = items.stream()
                .filter(candidate -> candidate.getId().equals(rentalItemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Položka " + rentalItemId + " ve výpůjčce č. " + id + " není."));
        item.markReturned(returnedAt);
        status = items.stream().anyMatch(RentalItem::isOut) ? RentalStatus.RETURNED_PARTIAL : RentalStatus.COMPLETED;
        return item;
    }

    /** Součet penále za všechny dosud vrácené kusy. */
    public BigDecimal getTotalLateFee() {
        return items.stream().map(RentalItem::getLateFee).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getPlannedStart() {
        return plannedStart;
    }

    public LocalDateTime getPlannedEnd() {
        return plannedEnd;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    /** Sleva je na uvážení vedoucího, jen nesmí být záporná. */
    public void setDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount.signum() < 0) {
            throw new DomainException("Sleva nemůže být záporná.");
        }
        this.discountAmount = discountAmount;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<RentalItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
