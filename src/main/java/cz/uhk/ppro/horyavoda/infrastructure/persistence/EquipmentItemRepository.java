package cz.uhk.ppro.horyavoda.infrastructure.persistence;

import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.ItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, Long>, JpaSpecificationExecutor<EquipmentItem> {

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<EquipmentItem> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "category")
    List<EquipmentItem> findAll(Specification<EquipmentItem> specification, Sort sort);

    boolean existsByInventoryNumber(String inventoryNumber);

    /**
     * Načte kusy se zámkem pro zápis. Dvě souběžné výpůjčky téhož kusu se tak seřadí
     * za sebou a druhá už uvidí první – překryv termínů nevznikne ani při souběhu.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EquipmentItem e join fetch e.category where e.id in :ids order by e.id")
    List<EquipmentItem> findAllByIdForUpdate(Collection<Long> ids);

    /**
     * Filtrovaný přehled vybavení seřazený podle inventárního čísla.
     * Každý filtr je volitelný (null = nefiltrovat).
     */
    default List<EquipmentItem> search(Long categoryId, ItemStatus status, String searchText) {
        Specification<EquipmentItem> specification = Specification.unrestricted();
        if (categoryId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (searchText != null && !searchText.isBlank()) {
            String pattern = "%" + searchText.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("inventoryNumber")), pattern),
                    cb.like(cb.lower(root.get("modelName")), pattern)
            ));
        }
        return findAll(specification, Sort.by("inventoryNumber"));
    }
}
