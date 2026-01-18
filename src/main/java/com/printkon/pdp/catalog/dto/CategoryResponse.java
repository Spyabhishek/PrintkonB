package com.printkon.pdp.catalog.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
	private String categoryId;
	private String name;
	private String slug;
	private String description;
	private String thumbnailUrl;
	private String bannerUrl;
	private Boolean active;
	private Integer displayOrder;
	private Long productCount;
	private String parentCategoryId;
	private List<CategoryResponse> subCategories;
	private LocalDateTime createdAt;
}