package com.printkon.pdp.catalog.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateRequest {
	@Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
	private String name;

	@Size(max = 500, message = "Description cannot exceed 500 characters")
	private String description;

	private String slug;

	private Boolean active;

	private Integer displayOrder;

	private String parentCategoryId;
}