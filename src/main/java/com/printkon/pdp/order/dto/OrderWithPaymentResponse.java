package com.printkon.pdp.order.dto;

import lombok.Data;

@Data
public class OrderWithPaymentResponse {
	private OrderResponse order;
	private Object paymentInfo; // Payment gateway specific data

	public OrderWithPaymentResponse(OrderResponse order, Object paymentInfo) {
		this.order = order;
		this.paymentInfo = paymentInfo;
	}
}