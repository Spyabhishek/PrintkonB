package com.printkon.pdp.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequest {
	@NotBlank(message = "Product name is required")
	@Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
	private String name;

	@Size(max = 1000, message = "Description cannot exceed 1000 characters")
	private String description;

	@NotNull(message = "Price is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
	private BigDecimal price;

	@NotBlank(message = "Category ID is required")
	private String categoryId;

	@Builder.Default
	private Boolean available = true;

	@Min(value = 0, message = "Stock quantity cannot be negative")
	private Integer stockQuantity;

	private String sku;

	private List<@Pattern(regexp = "^(https?|ftp)://.*$", message = "Invalid image URL format") String> imageUrls;

	private String mainImageUrl;
}
