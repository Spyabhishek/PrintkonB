package com.printkon.pdp.operator;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.common.enums.OrderStatus;
import com.printkon.pdp.order.OrderService;
import com.printkon.pdp.order.dto.OrderResponse;
import com.printkon.pdp.user.UserDetailsImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/operator/orders")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
public class OperatorOrderController {

	private final OrderService orderService;

	@GetMapping
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getAssignedOrders(
			@AuthenticationPrincipal UserDetailsImpl userDetails) {
		return orderService.getOrdersAssignedToOperator(userDetails);
	}

	// UPDATED: Use orderId (String) instead of internal id (Long)
	@PutMapping("/{orderId}/status")
	public ResponseEntity<ResponseStructure<OrderResponse>> updateOrderStatus(@PathVariable String orderId,
			@RequestParam OrderStatus status, @AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestParam(required = false) String notes) {
		return orderService.updateOrderStatusByOrderId(orderId, status, userDetails, notes);
	}

	// UPDATED: Use orderId (String) instead of internal id (Long)
	@PutMapping("/{orderId}/in-production")
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderInProduction(@PathVariable String orderId,
			@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestParam(required = false) String notes) {
		return orderService.updateOrderStatusByOrderId(orderId, OrderStatus.IN_PRODUCTION, userDetails, notes);
	}

	// UPDATED: Use orderId (String) instead of internal id (Long) and use the
	// correct method
	@PutMapping("/{orderId}/ready-for-delivery")
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderReadyForDelivery(@PathVariable String orderId,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestParam(required = false) String preparationNotes) {
		return orderService.markOrderReadyForDeliveryByOrderId(orderId, userDetails, preparationNotes);
	}

	// UPDATED: Use orderId (String) instead of internal id (Long) and use the
	// correct method
	@PutMapping("/{orderId}/out-for-delivery")
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderOutForDelivery(@PathVariable String orderId,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestParam(required = false) String trackingNumber) {
		return orderService.markOrderOutForDeliveryByOrderId(orderId, userDetails, trackingNumber);
	}

	// Dashboard endpoints for operator
	@GetMapping("/in-production")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersInProduction() {
		return orderService.getOrdersInProduction();
	}

	@GetMapping("/ready-for-delivery")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersReadyForDelivery() {
		return orderService.getOrdersReadyForDelivery();
	}

	@GetMapping("/approved")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getApprovedOrders() {
		return orderService.getOrdersByStatus(OrderStatus.APPROVED);
	}
}