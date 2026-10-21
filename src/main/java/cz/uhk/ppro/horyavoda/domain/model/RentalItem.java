package cz.uhk.ppro.horyavoda.domain.model;

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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vazební entita pro M:N vztah mezi {@link Rental} a {@link EquipmentItem}.
 * Uchovává historický snapshot sjednané ceny a samostatný stav položky pro částečné vracení.
 */
@Entity
@Table(name = "rental_items")
public class RentalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_item_id", nullable = false)
    private EquipmentItem equipmentItem;

    /** Snapshot ceny sjednané při vzniku smlouvy – změna ceníku ji neovlivní. */
    @Column(name = "unit_daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitDailyPrice;

    @Column(name = "actual_returned_at")
    private LocalDateTime actualReturnedAt;

    @Column(name = "late_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RentalItemStatus status = RentalItemStatus.RENTED;

    @Column(name = "damage_notes", length = 2000)
    private String damageNotes;

    protected RentalItem() {
        // vyžadováno JPA
    }

    RentalItem(Rental rental, EquipmentItem equipmentItem, BigDecimal unitDailyPrice) {
        this.rental = rental;
        this.equipmentItem = equipmentItem;
        this.unitDailyPrice = unitDailyPrice;
    }

    public Long getId() {
        return id;
    }

    public Rental getRental() {
        return rental;
    }

    public EquipmentItem getEquipmentItem() {
        return equipmentItem;
    }

    public BigDecimal getUnitDailyPrice() {
        return unitDailyPrice;
    }

    public LocalDateTime getActualReturnedAt() {
        return actualReturnedAt;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public RentalItemStatus getStatus() {
        return status;
    }

    public String getDamageNotes() {
        return damageNotes;
    }
}
