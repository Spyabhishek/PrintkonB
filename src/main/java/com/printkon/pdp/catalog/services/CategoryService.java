package com.printkon.pdp.catalog.services;

import com.printkon.pdp.catalog.dto.*;
import com.printkon.pdp.catalog.models.Category;
import com.printkon.pdp.catalog.repositories.CategoryRepository;
import com.printkon.pdp.catalog.repositories.ProductRepository;
import com.printkon.pdp.common.dto.PagedResponse;
import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.exceptions.BusinessRuleException;
import com.printkon.pdp.exceptions.ResourceNotFoundException;
import com.printkon.pdp.storage.StorageService;
import com.printkon.pdp.storage.dto.ImageUploadResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;

    private static final int MAX_ID_GENERATION_ATTEMPTS = 5;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    // ------------------ CREATE ------------------
    public ResponseEntity<ResponseStructure<CategoryResponse>> createCategory(
            @Valid CategoryCreateRequest request,
            MultipartFile thumbnailImage,
            MultipartFile bannerImage) {
        
        log.info("Creating category with name: '{}'", request.getName());

        // Check for duplicate category name
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessRuleException("Category with name '" + request.getName() + "' already exists");
        }

        // Check for duplicate slug
        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessRuleException("Category with slug '" + slug + "' already exists");
        }

        try {
            Category category = mapToCategory(request);
            category.setSlug(slug);

            // Handle parent category
            if (request.getParentCategoryId() != null) {
                Category parentCategory = categoryRepository.findByCategoryIdAndActiveTrue(request.getParentCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.getParentCategoryId()));
                category.setParentCategory(parentCategory);
            }

            // Generate unique category ID
            for (int attempt = 0; attempt < MAX_ID_GENERATION_ATTEMPTS; attempt++) {
                try {
                    // Upload images
                    if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
                        ImageUploadResponse thumbnailResponse = storageService.store(thumbnailImage, "categories");
                        category.setThumbnailUrl(thumbnailResponse.getImageUrl());
                    }
                    
                    if (bannerImage != null && !bannerImage.isEmpty()) {
                        ImageUploadResponse bannerResponse = storageService.store(bannerImage, "categories");
                        category.setBannerUrl(bannerResponse.getImageUrl());
                    }

                    Category savedCategory = categoryRepository.save(category);
                    log.info("Category created successfully with ID: {} and categoryId: {}", 
                        savedCategory.getId(), savedCategory.getCategoryId());

                    return buildSuccessResponse("Category created successfully", 
                            mapToResponse(savedCategory), HttpStatus.CREATED);
                    
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    if (e.getMessage().contains("category_id") && attempt < MAX_ID_GENERATION_ATTEMPTS - 1) {
                        category.setCategoryId(generateCustomCategoryId());
                        log.warn("Category ID collision detected, regenerating... Attempt: {}", attempt + 1);
                    } else {
                        throw new BusinessRuleException("Failed to create category after " + 
                            MAX_ID_GENERATION_ATTEMPTS + " attempts due to ID collision");
                    }
                }
            }
            
            throw new BusinessRuleException("Failed to create category");
            
        } catch (IOException e) {
            log.error("Error uploading images for category: {}", request.getName(), e);
            throw new BusinessRuleException("Failed to upload category images: " + e.getMessage());
        }
    }

    // Overloaded method for backward compatibility
    public ResponseEntity<ResponseStructure<CategoryResponse>> createCategory(@Valid CategoryCreateRequest request) {
        return createCategory(request, null, null);
    }

    // ------------------ READ ------------------
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseStructure<CategoryResponse>> getCategoryById(String categoryId) {
        log.info("Fetching category with ID: {}", categoryId);

        Category category = categoryRepository.findByCategoryIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        log.info("Successfully fetched category: '{}' (ID: {})", category.getName(), category.getCategoryId());
        return buildSuccessResponse("Category fetched successfully", mapToResponse(category), HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ResponseStructure<CategoryResponse>> getCategoryBySlug(String slug) {
        log.info("Fetching category with slug: {}", slug);

        Category category = categoryRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));

        return buildSuccessResponse("Category fetched successfully", mapToResponse(category), HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ResponseStructure<List<CategoryResponse>>> getAllCategories(Boolean includeInactive) {
        log.info("Fetching all categories, includeInactive: {}", includeInactive);

        List<Category> categories;
        if (Boolean.TRUE.equals(includeInactive)) {
            categories = categoryRepository.findAll();
        } else {
            categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
        }

        List<CategoryResponse> responses = categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("Successfully fetched {} categories", responses.size());
        return buildSuccessResponse("Categories fetched successfully", responses, HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ResponseStructure<PagedResponse<CategoryResponse>>> getCategoriesPaginated(
            Integer page, Integer size, String sortBy, String sortDirection, Boolean includeInactive) {
        
        log.info("Fetching categories paginated - page: {}, size: {}, includeInactive: {}", page, size, includeInactive);

        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<Category> categoriesPage;

        if (Boolean.TRUE.equals(includeInactive)) {
            categoriesPage = categoryRepository.findAll(pageable);
        } else {
            categoriesPage = categoryRepository.findByActiveTrue(pageable);
        }

        PagedResponse<CategoryResponse> pagedResponse = convertToPagedResponse(categoriesPage);
        log.info("Successfully fetched {} categories out of {}", 
            pagedResponse.getContent().size(), pagedResponse.getTotalElements());
        
        return buildSuccessResponse("Categories fetched successfully", pagedResponse, HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ResponseStructure<List<CategoryResponse>>> getRootCategories() {
        log.info("Fetching root categories");

        List<Category> rootCategories = categoryRepository.findByParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc();
        List<CategoryResponse> responses = rootCategories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("Found {} root categories", responses.size());
        return buildSuccessResponse("Root categories fetched successfully", responses, HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ResponseStructure<List<CategoryResponse>>> getSubCategories(String parentCategoryId) {
        log.info("Fetching sub-categories for parent: {}", parentCategoryId);

        List<Category> subCategories = categoryRepository.findByParentCategoryCategoryIdAndActiveTrue(parentCategoryId);
        List<CategoryResponse> responses = subCategories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("Found {} sub-categories for parent: {}", responses.size(), parentCategoryId);
        return buildSuccessResponse("Sub-categories fetched successfully", responses, HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ResponseStructure<List<CategoryResponse>>> searchCategories(String query) {
        log.info("Searching categories with query: '{}'", query);

        if (query == null || query.trim().isEmpty()) {
            return buildSuccessResponse("Please provide a search query", List.of(), HttpStatus.BAD_REQUEST);
        }

        List<Category> categories = categoryRepository.findByNameContainingIgnoreCaseAndActiveTrue(query.trim());
        List<CategoryResponse> responses = categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("Found {} categories for query: '{}'", responses.size(), query);
        return buildSuccessResponse("Categories search completed", responses, HttpStatus.OK);
    }

    // ------------------ UPDATE ------------------
    public ResponseEntity<ResponseStructure<CategoryResponse>> updateCategory(
            String categoryId,
            @Valid CategoryUpdateRequest request,
            MultipartFile thumbnailImage,
            MultipartFile bannerImage) {
        
        log.info("Updating category with ID: {}", categoryId);

        Category category = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        try {
            boolean hasChanges = false;
            StringBuilder changesLog = new StringBuilder("Updated fields: ");

            // Update basic fields
            if (request.getName() != null && !request.getName().equals(category.getName())) {
                // Check for duplicate name
                if (categoryRepository.existsByNameAndIdNot(request.getName(), category.getId())) {
                    throw new BusinessRuleException("Category with name '" + request.getName() + "' already exists");
                }
                category.setName(request.getName());
                changesLog.append("name, ");
                hasChanges = true;
            }

            if (request.getDescription() != null && !request.getDescription().equals(category.getDescription())) {
                category.setDescription(request.getDescription());
                changesLog.append("description, ");
                hasChanges = true;
            }

            if (request.getSlug() != null && !request.getSlug().equals(category.getSlug())) {
                // Check for duplicate slug
                if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), category.getId())) {
                    throw new BusinessRuleException("Category with slug '" + request.getSlug() + "' already exists");
                }
                category.setSlug(request.getSlug());
                changesLog.append("slug, ");
                hasChanges = true;
            }

            if (request.getActive() != null && !request.getActive().equals(category.getActive())) {
                category.setActive(request.getActive());
                changesLog.append("active, ");
                hasChanges = true;
            }

            if (request.getDisplayOrder() != null && !request.getDisplayOrder().equals(category.getDisplayOrder())) {
                category.setDisplayOrder(request.getDisplayOrder());
                changesLog.append("displayOrder, ");
                hasChanges = true;
            }

            // Update parent category
            if (request.getParentCategoryId() != null) {
                if (!request.getParentCategoryId().equals(category.getParentCategory() != null ? 
                    category.getParentCategory().getCategoryId() : null)) {
                    
                    Category parentCategory = categoryRepository.findByCategoryIdAndActiveTrue(request.getParentCategoryId())
                            .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.getParentCategoryId()));
                    
                    // Prevent circular reference
                    if (isCircularReference(category, parentCategory)) {
                        throw new BusinessRuleException("Cannot set parent category as it would create a circular reference");
                    }
                    
                    category.setParentCategory(parentCategory);
                    changesLog.append("parentCategory, ");
                    hasChanges = true;
                }
            } else if (request.getParentCategoryId() == null && category.getParentCategory() != null) {
                // Remove parent category
                category.setParentCategory(null);
                changesLog.append("parentCategory (removed), ");
                hasChanges = true;
            }

            // Update thumbnail image
            if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
                // Delete old thumbnail if exists
                if (category.getThumbnailUrl() != null) {
                    storageService.delete(category.getThumbnailUrl());
                }
                ImageUploadResponse thumbnailResponse = storageService.store(thumbnailImage, "categories");
                category.setThumbnailUrl(thumbnailResponse.getImageUrl());
                changesLog.append("thumbnail, ");
                hasChanges = true;
            }

            // Update banner image
            if (bannerImage != null && !bannerImage.isEmpty()) {
                // Delete old banner if exists
                if (category.getBannerUrl() != null) {
                    storageService.delete(category.getBannerUrl());
                }
                ImageUploadResponse bannerResponse = storageService.store(bannerImage, "categories");
                category.setBannerUrl(bannerResponse.getImageUrl());
                changesLog.append("banner, ");
                hasChanges = true;
            }

            if (hasChanges) {
                Category savedCategory = categoryRepository.save(category);
                log.info("Category updated successfully (ID: {}). {}", savedCategory.getCategoryId(),
                        changesLog.toString().replaceAll(", $", ""));
                return buildSuccessResponse("Category updated successfully", mapToResponse(savedCategory), HttpStatus.OK);
            } else {
                log.info("No changes detected for category ID: {}", categoryId);
                return buildSuccessResponse("No changes detected", mapToResponse(category), HttpStatus.OK);
            }

        } catch (IOException e) {
            log.error("Error uploading images during category update (ID: {})", categoryId, e);
            throw new BusinessRuleException("Failed to upload category images: " + e.getMessage());
        }
    }

    // Overloaded method for backward compatibility
    public ResponseEntity<ResponseStructure<CategoryResponse>> updateCategory(
            String categoryId, @Valid CategoryUpdateRequest request) {
        return updateCategory(categoryId, request, null, null);
    }

    // ------------------ DELETE ------------------
    public ResponseEntity<ResponseStructure<Void>> deleteCategory(String categoryId) {
        log.info("Soft deleting category with ID: {}", categoryId);

        Category category = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        if (!category.getActive()) {
            throw new BusinessRuleException("Category is already deleted");
        }

        // Check if category has active products
        long activeProductCount = productRepository.countByCategoryId(categoryId);
        if (activeProductCount > 0) {
            throw new BusinessRuleException("Cannot delete category with " + activeProductCount + " active products. Please reassign or delete products first.");
        }

        category.setActive(false);
        categoryRepository.save(category);

        log.info("Category soft deleted successfully: {}", categoryId);
        return buildSuccessResponse("Category deleted successfully", null, HttpStatus.OK);
    }

    public ResponseEntity<ResponseStructure<Void>> hardDeleteCategory(String categoryId) {
        log.info("Hard deleting category with ID: {}", categoryId);

        Category category = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        // Check if category has any products
        long productCount = productRepository.countByCategoryId(categoryId);
        if (productCount > 0) {
            throw new BusinessRuleException("Cannot delete category with " + productCount + " products. Please delete products first.");
        }

        // Delete associated images
        deleteCategoryImages(category);

        categoryRepository.delete(category);

        log.info("Category hard deleted successfully: {}", categoryId);
        return buildSuccessResponse("Category permanently deleted", null, HttpStatus.OK);
    }

    // ------------------ IMAGE MANAGEMENT ------------------
    public ResponseEntity<ResponseStructure<CategoryResponse>> updateCategoryThumbnail(
            String categoryId, MultipartFile thumbnailImage) {
        log.info("Updating thumbnail for category: {}", categoryId);

        Category category = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        try {
            // Delete old thumbnail if exists
            if (category.getThumbnailUrl() != null) {
                storageService.delete(category.getThumbnailUrl());
            }

            ImageUploadResponse thumbnailResponse = storageService.store(thumbnailImage, "categories");
            category.setThumbnailUrl(thumbnailResponse.getImageUrl());

            Category savedCategory = categoryRepository.save(category);
            log.info("Thumbnail updated for category: {}", categoryId);

            return buildSuccessResponse("Thumbnail updated successfully", mapToResponse(savedCategory), HttpStatus.OK);

        } catch (IOException e) {
            log.error("Failed to upload thumbnail for category: {} - Error: {}", categoryId, e.getMessage(), e);
            throw new BusinessRuleException("Failed to upload thumbnail: " + e.getMessage());
        }
    }

    public ResponseEntity<ResponseStructure<CategoryResponse>> updateCategoryBanner(
            String categoryId, MultipartFile bannerImage) {
        log.info("Updating banner for category: {}", categoryId);

        Category category = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        try {
            // Delete old banner if exists
            if (category.getBannerUrl() != null) {
                storageService.delete(category.getBannerUrl());
            }

            ImageUploadResponse bannerResponse = storageService.store(bannerImage, "categories");
            category.setBannerUrl(bannerResponse.getImageUrl());

            Category savedCategory = categoryRepository.save(category);
            log.info("Banner updated for category: {}", categoryId);

            return buildSuccessResponse("Banner updated successfully", mapToResponse(savedCategory), HttpStatus.OK);

        } catch (IOException e) {
            log.error("Failed to upload banner for category: {} - Error: {}", categoryId, e.getMessage(), e);
            throw new BusinessRuleException("Failed to upload banner: " + e.getMessage());
        }
    }

    // ------------------ HELPER METHODS ------------------
    private Category mapToCategory(CategoryCreateRequest request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive())
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    private CategoryResponse mapToResponse(Category category) {
        Long productCount = productRepository.countByCategoryId(category.getCategoryId());
        List<CategoryResponse> subCategoryResponses = category.getSubCategories() != null ?
                category.getSubCategories().stream()
                        .filter(Category::getActive)
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()) : List.of();

        String parentCategoryId = category.getParentCategory() != null ? 
                category.getParentCategory().getCategoryId() : null;

        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .thumbnailUrl(category.getThumbnailUrl())
                .bannerUrl(category.getBannerUrl())
                .active(category.getActive())
                .displayOrder(category.getDisplayOrder())
                .productCount(productCount)
                .parentCategoryId(parentCategoryId)
                .subCategories(subCategoryResponses)
                .createdAt(category.getCreatedAt())
                .build();
    }

    private boolean isCircularReference(Category category, Category potentialParent) {
        // Check if setting potentialParent as parent would create a circular reference
        Category current = potentialParent;
        while (current != null) {
            if (current.getId().equals(category.getId())) {
                return true;
            }
            current = current.getParentCategory();
        }
        return false;
    }

    private void deleteCategoryImages(Category category) {
        if (category.getThumbnailUrl() != null) {
            try {
                storageService.delete(category.getThumbnailUrl());
            } catch (Exception e) {
                log.warn("Failed to delete thumbnail image for category: {}", category.getCategoryId(), e);
            }
        }
        if (category.getBannerUrl() != null) {
            try {
                storageService.delete(category.getBannerUrl());
            } catch (Exception e) {
                log.warn("Failed to delete banner image for category: {}", category.getCategoryId(), e);
            }
        }
    }

    private String generateCustomCategoryId() {
        String prefix = "CAT";
        String random = String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        return prefix + random;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private Pageable createPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        int pageNumber = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        
        Sort.Direction direction = Sort.Direction.fromString(
            sortDirection != null ? sortDirection.toUpperCase() : "ASC");
        String sortField = sortBy != null ? sortBy : "displayOrder";
        
        Sort sort = Sort.by(direction, sortField);
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    private PagedResponse<CategoryResponse> convertToPagedResponse(Page<Category> categoriesPage) {
        List<CategoryResponse> content = categoriesPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<CategoryResponse>builder()
                .content(content)
                .pageNumber(categoriesPage.getNumber())
                .pageSize(categoriesPage.getSize())
                .totalElements(categoriesPage.getTotalElements())
                .totalPages(categoriesPage.getTotalPages())
                .last(categoriesPage.isLast())
                .first(categoriesPage.isFirst())
                .build();
    }

    private <T> ResponseEntity<ResponseStructure<T>> buildSuccessResponse(String message, T data, HttpStatus status) {
        ResponseStructure<T> structure = ResponseStructure.<T>builder()
                .success(true)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(structure);
    }
}