package com.printkon.pdp.order.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {
	private String productName;
	private Integer quantity;
	private String size;
	private String customNote;
	private BigDecimal unitPrice;
	private BigDecimal totalPrice;
}
