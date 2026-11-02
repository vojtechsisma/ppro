package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.domain.exception.EntityAlreadyExistsException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityNotFoundException;
import cz.uhk.ppro.horyavoda.domain.exception.HistoryProtectedException;
import cz.uhk.ppro.horyavoda.domain.exception.ItemInServiceException;
import cz.uhk.ppro.horyavoda.domain.model.Category;
import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.ItemStatus;
import cz.uhk.ppro.horyavoda.domain.model.WearCondition;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.CategoryRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.EquipmentItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.RentalItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Správa kategorií a fyzických kusů vybavení včetně servisní blokace. */
@Service
@Transactional
public class EquipmentService {

    private final EquipmentItemRepository equipmentItemRepository;
    private final CategoryRepository categoryRepository;
    private final RentalItemRepository rentalItemRepository;

    public EquipmentService(
            EquipmentItemRepository equipmentItemRepository,
            CategoryRepository categoryRepository,
            RentalItemRepository rentalItemRepository
    ) {
        this.equipmentItemRepository = equipmentItemRepository;
        this.categoryRepository = categoryRepository;
        this.rentalItemRepository = rentalItemRepository;
    }

    // --- KATEGORIE ---

    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category getCategory(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Kategorie s ID " + categoryId + " nebyla nalezena."));
    }

    public Category createCategory(String name, String code, BigDecimal baseDailyRate, BigDecimal weekendRate, String description) {
        String normalizedCode = code.strip().toUpperCase(Locale.ROOT);
        if (categoryRepository.existsByCode(normalizedCode)) {
            throw new EntityAlreadyExistsException("Kategorie s kódem '" + normalizedCode + "' již existuje.");
        }
        if (categoryRepository.existsByName(name)) {
            throw new EntityAlreadyExistsException("Kategorie s názvem '" + name + "' již existuje.");
        }
        return categoryRepository.save(new Category(name, normalizedCode, description, baseDailyRate, weekendRate));
    }

    // --- VYBAVENÍ ---

    @Transactional(readOnly = true)
    public List<EquipmentItem> listEquipment(Long categoryId, ItemStatus status, String search) {
        return equipmentItemRepository.search(categoryId, status, search);
    }

    @Transactional(readOnly = true)
    public EquipmentItem getEquipment(long itemId) {
        return equipmentItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Vybavení s ID " + itemId + " nebylo nalezeno."));
    }

    /** Zaevidování nového kusu; inventární číslo je unikátní a ukládá se velkými písmeny. */
    public EquipmentItem createEquipment(
            long categoryId,
            String inventoryNumber,
            String modelName,
            String size,
            int yearAcquired,
            WearCondition wearCondition,
            ItemStatus status
    ) {
        Category category = getCategory(categoryId);
        String normalizedInventoryNumber = inventoryNumber.strip().toUpperCase(Locale.ROOT);
        if (equipmentItemRepository.existsByInventoryNumber(normalizedInventoryNumber)) {
            throw new EntityAlreadyExistsException(
                    "Kus s inventárním číslem '" + normalizedInventoryNumber + "' již v systému existuje.");
        }
        EquipmentItem item = new EquipmentItem(
                category, normalizedInventoryNumber, modelName.strip(), size.strip(), yearAcquired, wearCondition, status);
        return equipmentItemRepository.save(item);
    }

    /** Částečná úprava kusu – mění se jen zadané (nenullové) hodnoty. */
    public EquipmentItem updateEquipment(long itemId, String modelName, String size, WearCondition wearCondition, ItemStatus status) {
        EquipmentItem item = getEquipment(itemId);
        if (modelName != null) {
            item.setModelName(modelName.strip());
        }
        if (size != null) {
            item.setSize(size.strip());
        }
        if (wearCondition != null) {
            item.setWearCondition(wearCondition);
        }
        if (status != null) {
            item.setStatus(status);
        }
        return item;
    }

    /** Převedení kusu do servisní blokace (IN_SERVICE). */
    public EquipmentItem sendToService(long itemId) {
        EquipmentItem item = getEquipment(itemId);
        item.sendToService();
        return item;
    }

    /**
     * Smazání kusu z fondu – jen pro omylem zaevidovaný kus bez historie.
     * Kus, který se už půjčoval, se nemaže (historie slouží k reklamacím a obměně), ale vyřadí.
     */
    public void deleteEquipment(long itemId) {
        EquipmentItem item = getEquipment(itemId);
        if (item.isRented()) {
            throw new ItemInServiceException("Nelze smazat kus, který je právě zapůjčen zákazníkovi.");
        }
        if (rentalItemRepository.existsByEquipmentItemId(itemId)) {
            throw new HistoryProtectedException("Kus " + item.getInventoryNumber()
                    + " má historii výpůjček, proto se nemaže. Vyřaďte ho změnou stavu na Vyřazeno.");
        }
        equipmentItemRepository.delete(item);
        equipmentItemRepository.flush();
    }
}
