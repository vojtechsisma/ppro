package cz.uhk.ppro.horyavoda.infrastructure.persistence;

import cz.uhk.ppro.horyavoda.domain.model.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {

    /** Všechny položky výpůjček, ve kterých se dané kusy kdy objevily (kontrola překryvu termínů). */
    @Query("select ri from RentalItem ri join fetch ri.rental join fetch ri.equipmentItem where ri.equipmentItem.id in :equipmentItemIds")
    List<RentalItem> findAllByEquipmentItemIds(Collection<Long> equipmentItemIds);

    boolean existsByEquipmentItemId(long equipmentItemId);
}
