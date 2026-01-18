package com.printkon.pdp.catalog.repositories;

import com.printkon.pdp.catalog.models.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

	// User-facing ID queries
	Optional<Category> findByCategoryId(String categoryId);

	Optional<Category> findByCategoryIdAndActiveTrue(String categoryId);

	boolean existsByCategoryId(String categoryId);

	// Internal ID queries
	Optional<Category> findByIdAndActiveTrue(Long id);

	// Name and slug queries
	Optional<Category> findByName(String name);

	Optional<Category> findBySlug(String slug);

	Optional<Category> findBySlugAndActiveTrue(String slug);

	List<Category> findByNameContainingIgnoreCase(String name);

	List<Category> findByNameContainingIgnoreCaseAndActiveTrue(String name);

	// Hierarchy queries
	List<Category> findByParentCategoryIsNullAndActiveTrue();

	List<Category> findByParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc();

	List<Category> findByParentCategoryCategoryIdAndActiveTrue(String parentCategoryId);

	// Active categories with products
	@Query("SELECT c FROM Category c WHERE c.active = true AND c.id IN "
			+ "(SELECT DISTINCT p.category.id FROM Product p WHERE p.available = true)")
	List<Category> findActiveCategoriesWithAvailableProducts();

	@Query("SELECT c FROM Category c LEFT JOIN FETCH c.products p WHERE c.active = true AND p.available = true")
	List<Category> findActiveCategoriesWithProducts();

	// Count queries
	@Query("SELECT COUNT(c) FROM Category c WHERE c.active = true")
	long countActiveCategories();

	boolean existsByName(String name);

	boolean existsBySlug(String slug);

	// Display order queries
	List<Category> findByActiveTrueOrderByDisplayOrderAsc();

	// === ADD THESE METHODS FOR PAGINATION ===

	// Paginated query for active categories
	Page<Category> findByActiveTrue(Pageable pageable);

	// Paginated query for all categories (including inactive)
	// This is already provided by JpaRepository: Page<Category> findAll(Pageable
	// pageable);

	// Duplicate check methods for update operations
	boolean existsByNameAndIdNot(String name, Long id);

	boolean existsBySlugAndIdNot(String slug, Long id);

	// Additional useful methods
	List<Category> findByParentCategoryIsNull();

	List<Category> findByParentCategoryCategoryId(String parentCategoryId);
}