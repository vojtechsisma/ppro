package cz.uhk.ppro.horyavoda.infrastructure.persistence;

import cz.uhk.ppro.horyavoda.domain.model.Reservation;
import cz.uhk.ppro.horyavoda.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Override
    @EntityGraph(attributePaths = {"customer", "category", "rental"})
    Optional<Reservation> findById(Long id);

    @EntityGraph(attributePaths = {"customer", "category", "rental"})
    @Query("select r from Reservation r order by r.plannedStart asc, r.id asc")
    List<Reservation> findAllByStart();

    /** Platné rezervace kategorie (velikost filtruje volající, porovnává se bez ohledu na mezery a velikost písmen). */
    @EntityGraph(attributePaths = "category")
    List<Reservation> findByCategoryIdAndStatus(long categoryId, ReservationStatus status);

    List<Reservation> findByStatus(ReservationStatus status);
}
