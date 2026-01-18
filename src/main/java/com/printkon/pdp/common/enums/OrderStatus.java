package com.printkon.pdp.common.enums;

public enum OrderStatus {
	// Initial flow
	PENDING_PAYMENT, // User placed order, awaiting payment
	PAYMENT_CONFIRMED, // Payment successful

	// Admin review flow
	UNDER_REVIEW, // Order waiting for admin approval
	APPROVED, // Admin approved and assigned to operator
	REJECTED, // Admin rejected the order

	// Operator processing flow
	IN_PRODUCTION, // Operator started working
	READY_FOR_DELIVERY, // Order completed and ready to deliver
	OUT_FOR_DELIVERY, // Order is out for delivery

	// Final states
	DELIVERED, // Successfully delivered
	CANCELLED, // Cancelled by user/admin

	// Legacy (keep for backward compatibility if needed)
	PROCESSING, SHIPPED, RETURNED
}