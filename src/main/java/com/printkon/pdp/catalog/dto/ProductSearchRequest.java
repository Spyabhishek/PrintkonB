package com.printkon.pdp.catalog.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {
	private String query;
	private String categoryId;
	private BigDecimal minPrice;
	private BigDecimal maxPrice;
	private Boolean inStock;
	private Boolean available;
	private String sortBy;
	private String sortDirection;
	private Integer page;
	private Integer size;
}