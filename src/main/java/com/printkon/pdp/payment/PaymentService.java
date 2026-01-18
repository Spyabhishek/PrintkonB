package com.printkon.pdp.payment;

import com.printkon.pdp.order.models.Order;

public interface PaymentService {
	/**
	 * Create a payment request with provider and return provider-specific data
	 * (e.g., order id, payment page url, client token).
	 */
	Object createPaymentRequest(Order order);

	/**
	 * Verify payment callback payload and mark success/failure. Implement
	 * provider-specific signature verification here.
	 */
	boolean verifyPayment(String payload, String signature);
}
