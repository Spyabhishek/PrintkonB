package com.printkon.pdp.catalog.repositories;

import com.printkon.pdp.catalog.models.ProductStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductStatsRepository extends JpaRepository<ProductStats, Long> {

	Optional<ProductStats> findByProductId(Long productId);

	// DB-side atomic increments
	@Modifying
	@Transactional
	@Query("UPDATE ProductStats s SET s.viewsCount = s.viewsCount + 1, s.lastUpdated = CURRENT_TIMESTAMP WHERE s.productId = :productId")
	int incrementViews(@Param("productId") Long productId);

	@Modifying
	@Transactional
	@Query("UPDATE ProductStats s SET s.wishlistCount = s.wishlistCount + 1, s.lastUpdated = CURRENT_TIMESTAMP WHERE s.productId = :productId")
	int incrementWishlist(@Param("productId") Long productId);

	@Modifying
	@Transactional
	@Query("UPDATE ProductStats s SET s.salesCount = s.salesCount + :quantity, s.lastUpdated = CURRENT_TIMESTAMP WHERE s.productId = :productId")
	int incrementSales(@Param("productId") Long productId, @Param("quantity") Long quantity);

	@Modifying
	@Transactional
	@Query("UPDATE ProductStats s SET s.reviewCount = s.reviewCount + 1, s.averageRating = :newRating, s.lastUpdated = CURRENT_TIMESTAMP WHERE s.productId = :productId")
	int updateRating(@Param("productId") Long productId, @Param("newRating") Double newRating);

	// Trending products queries
	@Query("SELECT s FROM ProductStats s WHERE s.product.available = true ORDER BY (s.salesCount * 0.5 + s.viewsCount * 0.3 + s.wishlistCount * 0.2) DESC")
	List<ProductStats> findTopTrendingProducts(org.springframework.data.domain.Pageable pageable);

	@Query("SELECT s FROM ProductStats s WHERE s.product.available = true AND s.product.isForceTrending = true")
	List<ProductStats> findForceTrendingProducts();

	// Analytics queries
	@Query("SELECT SUM(s.salesCount) FROM ProductStats s WHERE s.product.available = true")
	Long getTotalSales();

	@Query("SELECT SUM(s.viewsCount) FROM ProductStats s WHERE s.product.available = true")
	Long getTotalViews();
}