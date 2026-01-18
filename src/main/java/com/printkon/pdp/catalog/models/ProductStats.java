package com.printkon.pdp.catalog.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_stats", indexes = {
		@Index(name = "idx_product_stats_product_id", columnList = "product_id", unique = true),
		@Index(name = "idx_product_stats_sales", columnList = "sales_count"),
		@Index(name = "idx_product_stats_views", columnList = "views_count"),
		@Index(name = "idx_product_stats_wishlist", columnList = "wishlist_count"),
		@Index(name = "idx_product_stats_last_updated", columnList = "last_updated"),
		@Index(name = "idx_product_stats_composite", columnList = "sales_count, views_count, wishlist_count") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
public class ProductStats {

	@Id
	private Long productId;

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_stats_product"))
	private Product product;

	@Column(name = "sales_count", nullable = false)
	@Builder.Default
	private Long salesCount = 0L;

	@Column(name = "views_count", nullable = false)
	@Builder.Default
	private Long viewsCount = 0L;

	@Column(name = "wishlist_count", nullable = false)
	@Builder.Default
	private Long wishlistCount = 0L;

	@Column(name = "review_count", nullable = false)
	@Builder.Default
	private Long reviewCount = 0L;

	@Column(name = "average_rating", precision = 3, scale = 2)
	private BigDecimal averageRating;

	@Column(name = "last_updated")
	private LocalDateTime lastUpdated;

	@Version
	private Long version;

	@PrePersist
	@PreUpdate
	public void updateTimestamps() {
		this.lastUpdated = LocalDateTime.now();
	}

	// Business methods
	public void incrementSales(Long quantity) {
		this.salesCount += quantity;
	}

	public void incrementViews() {
		this.viewsCount++;
	}

	public void incrementWishlist() {
		this.wishlistCount++;
	}

	public void updateRating(BigDecimal newRating, Long newReviewCount) {
		this.averageRating = newRating;
		this.reviewCount = newReviewCount;
	}

	public Double getPopularityScore() {
		return (salesCount * 0.5) + (viewsCount * 0.3) + (wishlistCount * 0.2);
	}
}