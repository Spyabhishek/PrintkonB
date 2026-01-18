package com.printkon.pdp.catalog;

import com.printkon.pdp.catalog.dto.*;
import com.printkon.pdp.catalog.services.CategoryService;
import com.printkon.pdp.common.dto.PagedResponse;
import com.printkon.pdp.common.dto.ResponseStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@CrossOrigin("*")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    // ------------------ PUBLIC ENDPOINTS ------------------
    @GetMapping
    public ResponseEntity<ResponseStructure<List<CategoryResponse>>> getAllCategories(
            @RequestParam(required = false) Boolean includeInactive) {
        return categoryService.getAllCategories(includeInactive);
    }

    @GetMapping("/paginated")
    public ResponseEntity<ResponseStructure<PagedResponse<CategoryResponse>>> getCategoriesPaginated(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false) Boolean includeInactive) {
        return categoryService.getCategoriesPaginated(page, size, sortBy, sortDirection, includeInactive);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ResponseStructure<CategoryResponse>> getCategoryById(
            @PathVariable @NotBlank String categoryId) {
        return categoryService.getCategoryById(categoryId);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ResponseStructure<CategoryResponse>> getCategoryBySlug(
            @PathVariable @NotBlank String slug) {
        return categoryService.getCategoryBySlug(slug);
    }

    @GetMapping("/root")
    public ResponseEntity<ResponseStructure<List<CategoryResponse>>> getRootCategories() {
        return categoryService.getRootCategories();
    }

    @GetMapping("/{parentCategoryId}/subcategories")
    public ResponseEntity<ResponseStructure<List<CategoryResponse>>> getSubCategories(
            @PathVariable @NotBlank String parentCategoryId) {
        return categoryService.getSubCategories(parentCategoryId);
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseStructure<List<CategoryResponse>>> searchCategories(
            @RequestParam @NotBlank String query) {
        return categoryService.searchCategories(query);
    }

    // ------------------ ADMIN ENDPOINTS ------------------
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseStructure<CategoryResponse>> createCategory(
            @Valid @RequestPart("category") CategoryCreateRequest request,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage,
            @RequestPart(value = "bannerImage", required = false) MultipartFile bannerImage) {

        log.info("Creating category with images - Name: {}, Has thumbnail: {}, Has banner: {}", 
            request.getName(), thumbnailImage != null, bannerImage != null);

        return categoryService.createCategory(request, thumbnailImage, bannerImage);
    }

    @PostMapping("/json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseStructure<CategoryResponse>> createCategoryJson(
            @Valid @RequestBody CategoryCreateRequest request) {
        log.info("Creating category without images - Name: {}", request.getName());
        return categoryService.createCategory(request);
    }

    @PutMapping(value = "/{categoryId}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseStructure<CategoryResponse>> updateCategory(
            @PathVariable @NotBlank String categoryId,
            @Valid @RequestPart("category") CategoryUpdateRequest request,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage,
            @RequestPart(value = "bannerImage", required = false) MultipartFile bannerImage) {

        log.info("Updating category with images - ID: {}, Has thumbnail: {}, Has banner: {}", 
            categoryId, thumbnailImage != null, bannerImage != null);

        return categoryService.updateCategory(categoryId, request, thumbnailImage, bannerImage);
    }

    @PutMapping("/json/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseStructure<CategoryResponse>> updateCategoryJson(
            @PathVariable @NotBlank String categoryId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        log.info("Updating category without images - ID: {}", categoryId);
        return categoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseStructure<Void>> deleteCategory(
            @PathVariable @NotBlank String categoryId) {
        return categoryService.deleteCategory(categoryId);
    }

    @DeleteMapping("/{categoryId}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseStructure<Void>> hardDeleteCategory(
            @PathVariable @NotBlank String categoryId) {
        return categoryService.hardDeleteCategory(categoryId);
    }

    // ------------------ IMAGE MANAGEMENT ------------------
    @PutMapping("/{categoryId}/thumbnail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseStructure<CategoryResponse>> updateThumbnail(
            @PathVariable @NotBlank String categoryId,
            @RequestParam("thumbnailImage") MultipartFile thumbnailImage) {
        return categoryService.updateCategoryThumbnail(categoryId, thumbnailImage);
    }

    @PutMapping("/{categoryId}/banner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseStructure<CategoryResponse>> updateBanner(
            @PathVariable @NotBlank String categoryId,
            @RequestParam("bannerImage") MultipartFile bannerImage) {
        return categoryService.updateCategoryBanner(categoryId, bannerImage);
    }
}