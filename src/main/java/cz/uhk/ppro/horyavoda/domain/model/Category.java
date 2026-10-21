package cz.uhk.ppro.horyavoda.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Kategorie vybavení (lyže, boty, lodě, ...) – určuje ceník denní a víkendové sazby. */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "base_daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseDailyRate;

    @Column(name = "weekend_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal weekendRate;

    protected Category() {
        // vyžadováno JPA
    }

    public Category(String name, String code, String description, BigDecimal baseDailyRate, BigDecimal weekendRate) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.baseDailyRate = baseDailyRate;
        this.weekendRate = weekendRate;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBaseDailyRate() {
        return baseDailyRate;
    }

    public BigDecimal getWeekendRate() {
        return weekendRate;
    }
}
