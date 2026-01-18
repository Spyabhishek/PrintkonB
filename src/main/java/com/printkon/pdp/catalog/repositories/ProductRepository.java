package com.printkon.pdp.catalog.repositories;

import com.printkon.pdp.catalog.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	// User-facing ID queries
	Optional<Product> findByProductId(String productId);

	Optional<Product> findByProductIdAndAvailableTrue(String productId);

	boolean existsByProductId(String productId);

	// Internal ID queries (for internal operations)
	Optional<Product> findByIdAndAvailableTrue(Long id);

	// Category queries with user-facing IDs
	@Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId AND p.available = true")
	List<Product> findByCategoryIdAndAvailableTrue(@Param("categoryId") String categoryId);

	@Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId AND p.available = true")
	Page<Product> findByCategoryIdAndAvailableTrue(@Param("categoryId") String categoryId, Pageable pageable);

	// Search and filter queries
	List<Product> findByAvailableTrue();

	Page<Product> findByAvailableTrue(Pageable pageable);

	@Query("SELECT p FROM Product p WHERE p.available = true AND "
			+ "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR "
			+ "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
	List<Product> searchByNameOrDescription(@Param("query") String query);

	@Query("SELECT p FROM Product p WHERE p.available = true AND "
			+ "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR "
			+ "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
	Page<Product> searchByNameOrDescription(@Param("query") String query, Pageable pageable);

	@Query("SELECT p FROM Product p WHERE p.available = true AND p.price BETWEEN :minPrice AND :maxPrice")
	List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

	@Query("SELECT p FROM Product p WHERE p.available = true AND p.stockQuantity > 0")
	List<Product> findInStockProducts();

	// Popular and trending products
	List<Product> findByIsPopularTrueAndAvailableTrue();

	List<Product> findByIsForceTrendingTrueAndAvailableTrue();

	// Count queries
	@Query("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :categoryId AND p.available = true")
	long countByCategoryId(@Param("categoryId") String categoryId);

	@Query("SELECT COUNT(p) FROM Product p WHERE p.available = true")
	long countAvailableProducts();

	// Bulk operations
	@Query("UPDATE Product p SET p.available = :available WHERE p.category.categoryId = :categoryId")
	int updateAvailabilityByCategoryId(@Param("categoryId") String categoryId, @Param("available") Boolean available);

	// Add to ProductRepository
	List<Product> findAllByProductIdIn(List<String> productIds);
}