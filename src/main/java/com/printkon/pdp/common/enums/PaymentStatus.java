package com.printkon.pdp.common.enums;

//Consider more descriptive payment statuses
public enum PaymentStatus {
	PENDING, // Waiting for payment
	AUTHORIZED, // Payment authorized but not captured
	PAID, // Payment successfully collected
	CANCELLED, // Payment cancelled before completion
	REFUND_PENDING, // Refund initiated
	REFUNDED, // Refund completed
	FAILED, // Payment failed
	EXPIRED // Payment never completed (like COD cancellation)
}