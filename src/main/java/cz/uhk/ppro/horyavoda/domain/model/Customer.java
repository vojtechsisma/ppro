package cz.uhk.ppro.horyavoda.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Zákazník půjčovny identifikovaný číslem dokladu totožnosti. */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "id_document_number", nullable = false, unique = true, length = 30)
    private String idDocumentNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Customer() {
        // vyžadováno JPA
    }

    public Customer(String firstName, String lastName, String phone, String idDocumentNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.idDocumentNumber = idDocumentNumber;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getIdDocumentNumber() {
        return idDocumentNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
