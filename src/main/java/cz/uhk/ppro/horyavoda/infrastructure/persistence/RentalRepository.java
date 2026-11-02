package cz.uhk.ppro.horyavoda.infrastructure.persistence;

import cz.uhk.ppro.horyavoda.domain.model.Rental;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    @Override
    @EntityGraph(attributePaths = {"customer", "items", "items.equipmentItem"})
    Optional<Rental> findById(Long id);

    @EntityGraph(attributePaths = {"customer", "items", "items.equipmentItem"})
    @Query("select r from Rental r order by r.plannedStart desc")
    List<Rental> findAllNewestFirst();

    /** Historie výpůjček jednoho kusu, nejnovější první. */
    @EntityGraph(attributePaths = {"customer", "items", "items.equipmentItem"})
    @Query("""
            select distinct r from Rental r join r.items ri
            where ri.equipmentItem.id = :equipmentItemId
            order by r.plannedStart desc
            """)
    List<Rental> findByEquipmentItemId(long equipmentItemId);
}
