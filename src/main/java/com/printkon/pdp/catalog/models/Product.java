package com.printkon.pdp.catalog.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "products", indexes = { @Index(name = "idx_product_product_id", columnList = "productId", unique = true),
		@Index(name = "idx_product_category_id", columnList = "category_id"),
		@Index(name = "idx_product_available", columnList = "available"),
		@Index(name = "idx_product_created_at", columnList = "created_at"),
		@Index(name = "idx_product_popular_trending", columnList = "is_popular, is_force_trending, available"),
		@Index(name = "idx_product_name", columnList = "name"),
		@Index(name = "idx_product_price", columnList = "price") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Product ID is required")
	@Size(min = 10, max = 10, message = "Product ID must be exactly 10 characters")
	@Column(name = "product_id", nullable = false, unique = true, length = 10)
	private String productId;

	@NotBlank(message = "Product name is required")
	@Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
	@Column(nullable = false)
	private String name;

	@Size(max = 1000, message = "Description cannot exceed 1000 characters")
	@Column(length = 1000)
	private String description;

	@NotNull(message = "Price is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Builder.Default
	@Column(nullable = false)
	private Boolean available = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_category"))
	@NotNull(message = "Category is required")
	private Category category;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"), indexes = @Index(name = "idx_product_images_product_id", columnList = "product_id"), foreignKey = @ForeignKey(name = "fk_product_images_product"))
	@Column(name = "image_url", length = 500)
	@Builder.Default
	private List<String> imageUrls = new ArrayList<>();

	@Column(name = "main_image_url", length = 500)
	private String mainImageUrl;

	@Column(name = "sku", length = 50)
	private String sku;

	@Column(name = "stock_quantity", nullable = false)
	@Builder.Default
	private Integer stockQuantity = 0;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "is_popular", nullable = false)
	@Builder.Default
	private Boolean isPopular = false;

	@Column(name = "is_force_trending", nullable = false)
	@Builder.Default
	private Boolean isForceTrending = false;

	@Version
	private Long version;

	@PrePersist
	protected void generateProductId() {
		if (this.productId == null) {
			this.productId = generateCustomProductId();
		}
	}

	private String generateCustomProductId() {
		String timestamp = String.valueOf(System.currentTimeMillis() % 1000000);
		String random = String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
		return "P" + timestamp + random;
	}

	// Business logic methods
	public boolean isInStock() {
		return this.available && this.stockQuantity > 0;
	}

	public void reduceStock(Integer quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive");
		}
		if (this.stockQuantity < quantity) {
			throw new IllegalStateException("Insufficient stock");
		}
		this.stockQuantity -= quantity;
	}

	public void addStock(Integer quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive");
		}
		this.stockQuantity += quantity;
	}

	public void addImageUrl(String imageUrl) {
		if (this.imageUrls == null) {
			this.imageUrls = new ArrayList<>();
		}
		this.imageUrls.add(imageUrl);

		if (this.mainImageUrl == null || this.mainImageUrl.isEmpty()) {
			this.mainImageUrl = imageUrl;
		}
	}

	public void removeImageUrl(String imageUrl) {
		if (this.imageUrls != null) {
			this.imageUrls.remove(imageUrl);
			if (imageUrl.equals(this.mainImageUrl)) {
				this.mainImageUrl = this.imageUrls.isEmpty() ? null : this.imageUrls.get(0);
			}
		}
	}

	public void setMainImageUrl(String imageUrl) {
		if (this.imageUrls != null && this.imageUrls.contains(imageUrl)) {
			this.mainImageUrl = imageUrl;
		}
	}
}