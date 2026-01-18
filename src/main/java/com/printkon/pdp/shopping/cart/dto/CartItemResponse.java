package com.printkon.pdp.shopping.cart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class CartItemResponse {
	private Long id;
	private String productId;
	private String productName;
	private BigDecimal productPrice;
	private Integer quantity;
	private Map<String, Object> options; 
	private String imageUrl;
	private Boolean available;
	private Boolean inStock;
}