package com.printkon.pdp.shopping.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CartItemRequest {
	@NotNull
	private String productId; // CHANGE: Long → String

	@Min(1)
	private Integer quantity = 1;

	private Map<String, Object> options;
}