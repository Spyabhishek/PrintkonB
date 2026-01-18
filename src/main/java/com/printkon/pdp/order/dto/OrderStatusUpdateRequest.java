package com.printkon.pdp.order.dto;

import lombok.Data;

import com.printkon.pdp.common.enums.OrderStatus;

@Data
public class OrderStatusUpdateRequest {
	private OrderStatus status;
	private String notes; // Optional notes about the status change
	private String trackingNumber; // For OUT_FOR_DELIVERY status
}