package com.printkon.pdp.shopping.cart.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartResponse {
	private Long cartId;
	private Long userId;
	private List<CartItemResponse> items;
	private BigDecimal totalAmount;
}