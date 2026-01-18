package com.printkon.pdp.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {
	@Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
	private String name;

	@Size(max = 1000, message = "Description cannot exceed 1000 characters")
	private String description;

	@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
	private BigDecimal price;

	private Boolean available;

	@Min(value = 0, message = "Stock quantity cannot be negative")
	private Integer stockQuantity;

	private String sku;

	private String categoryId;
}
