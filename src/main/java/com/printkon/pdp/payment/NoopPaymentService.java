package com.printkon.pdp.payment;

import org.springframework.stereotype.Service;
import com.printkon.pdp.order.models.Order;

@Service
public class NoopPaymentService implements PaymentService {

	@Override
	public Object createPaymentRequest(Order order) {
		// For dev, return a dummy client token / payment id
		return java.util.Map.of("providerOrderId", "NOOP-" + order.getId(), "amount", order.getOrderTotal());
	}

	@Override
	public boolean verifyPayment(String payload, String signature) {
		// Accept everything in dev
		return true;
	}
}
