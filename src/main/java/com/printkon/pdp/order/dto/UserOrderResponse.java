package com.printkon.pdp.order.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.printkon.pdp.order.models.ShippingAddress;

@Data
@Builder
public class UserOrderResponse {
	private String orderId; // NEW: Primary external identifier
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	private BigDecimal orderTotal;
	private String paymentMethod;
	private String paymentStatus;

	private ShippingAddress shippingAddress;
	private String deliveryInstructions;
	private LocalDate estimatedDeliveryDate;
	private String trackingNumber;

	private String cancellationReason;
	private LocalDateTime cancelledAt;

	private List<UserOrderItemResponse> items;
	private List<OrderEventResponse> orderEvents;
}