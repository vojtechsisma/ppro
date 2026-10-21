package cz.uhk.ppro.horyavoda.presentation.api;

import cz.uhk.ppro.horyavoda.application.service.EquipmentService;
import cz.uhk.ppro.horyavoda.domain.model.ItemStatus;
import cz.uhk.ppro.horyavoda.presentation.dto.EquipmentCreateRequest;
import cz.uhk.ppro.horyavoda.presentation.dto.EquipmentResponse;
import cz.uhk.ppro.horyavoda.presentation.dto.EquipmentUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    /**
     * Přehled vybavení (veřejné). Umožňuje ranní pultové filtrování,
     * např. {@code ?status=AVAILABLE} pro volné kusy v sobotu ráno.
     */
    @GetMapping
    public List<EquipmentResponse> listEquipment(
            @RequestParam(name = "category_id", required = false) Long categoryId,
            @RequestParam(name = "status", required = false) ItemStatus status,
            @RequestParam(name = "search", required = false) String search
    ) {
        return equipmentService.listEquipment(categoryId, status, search).stream().map(EquipmentResponse::from).toList();
    }

    /** Zaevidování nového kusu vybavení (vyžaduje přihlášení). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse createEquipment(@Valid @RequestBody EquipmentCreateRequest request) {
        return EquipmentResponse.from(equipmentService.createEquipment(
                request.categoryId(), request.inventoryNumber(), request.modelName(), request.size(),
                request.yearAcquired(), request.wearConditionOrDefault(), request.statusOrDefault()));
    }

    /** Detail konkrétního kusu vybavení (veřejné). */
    @GetMapping("/{itemId}")
    public EquipmentResponse getEquipment(@PathVariable long itemId) {
        return EquipmentResponse.from(equipmentService.getEquipment(itemId));
    }

    /** Úprava parametrů nebo stavu kusu (vyžaduje přihlášení). */
    @PutMapping("/{itemId}")
    public EquipmentResponse updateEquipment(@PathVariable long itemId, @Valid @RequestBody EquipmentUpdateRequest request) {
        return EquipmentResponse.from(equipmentService.updateEquipment(
                itemId, request.modelName(), request.size(), request.wearCondition(), request.status()));
    }

    /** Převedení kusu do servisní blokace IN_SERVICE (vyžaduje přihlášení). */
    @PostMapping("/{itemId}/service")
    public EquipmentResponse sendToService(@PathVariable long itemId) {
        return EquipmentResponse.from(equipmentService.sendToService(itemId));
    }

    /** Smazání kusu z fondu; zapůjčený kus smazat nelze (vyžaduje přihlášení). */
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEquipment(@PathVariable long itemId) {
        equipmentService.deleteEquipment(itemId);
    }
}
