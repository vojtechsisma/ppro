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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Konkrétní fyzický kus vybavení s unikátním inventárním číslem.
 * Dva totožné páry lyží téhož modelu a délky jsou dva samostatné záznamy.
 */
@Entity
@Table(name = "equipment_items")
public class EquipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "inventory_number", nullable = false, unique = true, length = 50)
    private String inventoryNumber;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "size", nullable = false, length = 30)
    private String size;

    @Column(name = "year_acquired", nullable = false)
    private int yearAcquired;

    @Enumerated(EnumType.STRING)
    @Column(name = "wear_condition", nullable = false, length = 20)
    private WearCondition wearCondition = WearCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ItemStatus status = ItemStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected EquipmentItem() {
        // vyžadováno JPA
    }

    public EquipmentItem(
            Category category,
            String inventoryNumber,
            String modelName,
            String size,
            int yearAcquired,
            WearCondition wearCondition,
            ItemStatus status
    ) {
        this.category = category;
        this.inventoryNumber = inventoryNumber;
        this.modelName = modelName;
        this.size = size;
        this.yearAcquired = yearAcquired;
        this.wearCondition = wearCondition;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Převedení kusu do servisní blokace – po dobu servisu se nesmí půjčit ani rezervovat. */
    public void sendToService() {
        this.status = ItemStatus.IN_SERVICE;
    }

    public boolean isRented() {
        return status == ItemStatus.RENTED;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getInventoryNumber() {
        return inventoryNumber;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getYearAcquired() {
        return yearAcquired;
    }

    public WearCondition getWearCondition() {
        return wearCondition;
    }

    public void setWearCondition(WearCondition wearCondition) {
        this.wearCondition = wearCondition;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
