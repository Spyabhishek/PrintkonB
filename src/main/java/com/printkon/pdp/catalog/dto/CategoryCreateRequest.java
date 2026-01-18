package com.printkon.pdp.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCreateRequest {
	@NotBlank(message = "Category name is required")
	@Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
	private String name;

	@Size(max = 500, message = "Description cannot exceed 500 characters")
	private String description;

	private String slug;

	@Builder.Default
	private Boolean active = true;

	@Builder.Default
	private Integer displayOrder = 0;

	private String parentCategoryId;
}