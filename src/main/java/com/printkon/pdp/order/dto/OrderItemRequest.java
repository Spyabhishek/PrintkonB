package com.printkon.pdp.order.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
	private String productId;
	private Integer quantity;
	private String size;
	private String customNote;
}
