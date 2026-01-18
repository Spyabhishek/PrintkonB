package com.printkon.pdp.order.dto;

import java.util.List;

import com.printkon.pdp.common.enums.PaymentMethod;
import com.printkon.pdp.order.models.ShippingAddress;

import lombok.Data;

@Data
public class OrderRequest {
	private List<OrderItemRequest> items;
	// either pick an existing address or send inline
	private Long shippingAddressId; // optional: existing saved address
	private ShippingAddress shippingAddress; // optional inline
	private PaymentMethod paymentMethod;
	private String deliveryInstructions;
}
