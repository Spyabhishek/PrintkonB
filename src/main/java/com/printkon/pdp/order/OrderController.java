package com.printkon.pdp.order;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.common.enums.OrderStatus;
import com.printkon.pdp.order.dto.*;
import com.printkon.pdp.user.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	// ========== CUSTOMER ENDPOINTS ==========

	@PostMapping
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<ResponseStructure<OrderResponse>> placeOrder(@RequestBody OrderRequest request,
			@AuthenticationPrincipal UserDetailsImpl userDetails) {
		return orderService.placeOrder(request, userDetails);
	}

	// UPDATED: Use orderId in path
	@GetMapping("/my/{orderId}")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<ResponseStructure<UserOrderResponse>> getMyOrderById(@PathVariable String orderId,
			@AuthenticationPrincipal UserDetailsImpl userDetails) {
		return orderService.getMyOrderByOrderId(orderId, userDetails);
	}

	@GetMapping("/my")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<ResponseStructure<List<UserOrderResponse>>> getMyOrders(
			@AuthenticationPrincipal UserDetailsImpl userDetails) {
		return orderService.getMyOrders(userDetails);
	}

	// UPDATED: Use orderId for cancellation
	@PostMapping("/{orderId}/cancel")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<ResponseStructure<UserOrderResponse>> cancelOrder(@PathVariable String orderId,
			@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody CancelOrderRequest cancelRequest) {
		return orderService.cancelOrderByOrderId(orderId, userDetails, cancelRequest);
	}

	// ========== PAYMENT ENDPOINTS ==========

	@PostMapping("/payment/confirm")
	public ResponseEntity<ResponseStructure<OrderResponse>> confirmPayment(
			@RequestBody PaymentConfirmationRequest request) {
		return orderService.confirmPaymentByOrderId(request);
	}

	// ========== ADMIN/OPERATOR ENDPOINTS ==========

	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getAllOrders(
			@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestParam(required = false) String status) {
		return orderService.getAllOrders(userDetails, status);
	}

	// UPDATED: Use orderId in admin endpoints
	@PostMapping("/{orderId}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<OrderResponse>> approveOrder(@PathVariable String orderId,
			@RequestBody ApproveOrderRequest approveRequest, @AuthenticationPrincipal UserDetailsImpl currentUser) {
		return orderService.approveOrderByOrderId(orderId, approveRequest, currentUser);
	}

	@PostMapping("/{orderId}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<OrderResponse>> rejectOrder(@PathVariable String orderId,
			@RequestParam String reason, @AuthenticationPrincipal UserDetailsImpl currentUser) {
		return orderService.rejectOrderByOrderId(orderId, reason, currentUser);
	}

	// Admin dashboard endpoints
	@GetMapping("/under-review")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersUnderReview() {
		return orderService.getOrdersUnderReview();
	}

	// ========== OPERATOR ENDPOINTS ==========

	@GetMapping("/assigned")
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersAssignedToOperator(
			@AuthenticationPrincipal UserDetailsImpl userDetails) {
		return orderService.getOrdersAssignedToOperator(userDetails);
	}

	// UPDATED: Use orderId in operator endpoints
	@PutMapping("/{orderId}/status")
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<OrderResponse>> updateOrderStatus(@PathVariable String orderId,
			@RequestParam OrderStatus status, @AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestParam(required = false) String notes) {
		return orderService.updateOrderStatusByOrderId(orderId, status, userDetails, notes);
	}

	@PostMapping("/{orderId}/ready-for-delivery")
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderReadyForDelivery(@PathVariable String orderId,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestParam(required = false) String preparationNotes) {
		return orderService.markOrderReadyForDeliveryByOrderId(orderId, userDetails, preparationNotes);
	}

	@PostMapping("/{orderId}/out-for-delivery")
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderOutForDelivery(@PathVariable String orderId,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestParam(required = false) String trackingNumber) {
		return orderService.markOrderOutForDeliveryByOrderId(orderId, userDetails, trackingNumber);
	}

	@PostMapping("/{orderId}/delivered")
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderDelivered(@PathVariable String orderId) {
		return orderService.markOrderDeliveredByOrderId(orderId);
	}

	// Operator dashboard endpoints
	@GetMapping("/in-production")
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersInProduction() {
		return orderService.getOrdersInProduction();
	}

	@GetMapping("/ready-for-delivery")
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersReadyForDelivery() {
		return orderService.getOrdersReadyForDelivery();
	}
}