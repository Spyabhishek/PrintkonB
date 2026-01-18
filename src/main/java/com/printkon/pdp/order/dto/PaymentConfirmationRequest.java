package com.printkon.pdp.order.dto;

import lombok.Data;

@Data
public class PaymentConfirmationRequest {
	private String orderId; // NEW: External reference
	private String paymentProvider;
	private String providerPaymentId;
	private String status;
	private String rawPayload;
}