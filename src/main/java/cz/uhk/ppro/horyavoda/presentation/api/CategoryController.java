package cz.uhk.ppro.horyavoda.presentation.api;

import cz.uhk.ppro.horyavoda.application.service.EquipmentService;
import cz.uhk.ppro.horyavoda.presentation.dto.CategoryCreateRequest;
import cz.uhk.ppro.horyavoda.presentation.dto.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final EquipmentService equipmentService;

    public CategoryController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    /** Seznam všech kategorií vybavení (veřejné). */
    @GetMapping
    public List<CategoryResponse> listCategories() {
        return equipmentService.listCategories().stream().map(CategoryResponse::from).toList();
    }

    /** Vytvoření nové kategorie (vyžaduje přihlášení). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return CategoryResponse.from(equipmentService.createCategory(
                request.name(), request.code(), request.baseDailyRate(), request.weekendRate(), request.description()));
    }

    /** Detail kategorie (veřejné). */
    @GetMapping("/{categoryId}")
    public CategoryResponse getCategory(@PathVariable long categoryId) {
        return CategoryResponse.from(equipmentService.getCategory(categoryId));
    }
}
