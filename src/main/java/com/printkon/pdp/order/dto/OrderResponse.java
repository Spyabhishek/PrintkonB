package com.printkon.pdp.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.printkon.pdp.common.enums.PaymentMethod;
import com.printkon.pdp.common.enums.PaymentStatus;
import com.printkon.pdp.order.models.ShippingAddress;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
	private String orderId; // NEW: External identifier (replaces id for external use)
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	private BigDecimal orderTotal;
	private PaymentMethod paymentMethod;
	private PaymentStatus paymentStatus;

	private ShippingAddress shippingAddress;
	private String deliveryInstructions;
	private LocalDate estimatedDeliveryDate;
	private String trackingNumber;

	private String rejectionReason;
	private String cancellationReason;
	private LocalDateTime cancelledAt;

	private String customerName;
	private String assignedOperatorName;
	private String approvedByName;

	private List<OrderItemResponse> items;
}