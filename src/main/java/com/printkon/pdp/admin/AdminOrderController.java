package com.printkon.pdp.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.common.enums.OrderStatus;
import com.printkon.pdp.operator.OperatorService;
import com.printkon.pdp.order.OrderService;
import com.printkon.pdp.order.dto.ApproveOrderRequest;
import com.printkon.pdp.order.dto.OrderResponse;
import com.printkon.pdp.user.UserDetailsImpl;
import com.printkon.pdp.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

	private final OrderService orderService;
	private final OperatorService operatorService;

	@GetMapping("/orders")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getAllOrders(
			@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestParam(required = false) String status) {
		return orderService.getAllOrders(userDetails, status);
	}

	@GetMapping("/orders/under-review")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersUnderReview() {
		return orderService.getOrdersUnderReview();
	}

	// FIXED: Use getOrdersByStatus with OrderStatus enum instead of getOrders with
	// String
	@GetMapping("/orders/filter-by-status")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersByStatus(@RequestParam OrderStatus status) {
		return orderService.getOrdersByStatus(status);
	}

	// UPDATED: Use orderId (String) instead of internal id (Long)
	@PostMapping("/orders/{orderId}/approve")
	public ResponseEntity<ResponseStructure<OrderResponse>> approveOrder(@PathVariable String orderId,
			@RequestBody ApproveOrderRequest approveRequest, @AuthenticationPrincipal UserDetailsImpl userDetails) {
		return orderService.approveOrderByOrderId(orderId, approveRequest, userDetails);
	}

	// UPDATED: Use orderId (String) instead of internal id (Long)
	@PostMapping("/orders/{orderId}/reject")
	public ResponseEntity<ResponseStructure<OrderResponse>> rejectOrder(@PathVariable String orderId,
			@RequestParam String reason, @AuthenticationPrincipal UserDetailsImpl userDetails) {
		return orderService.rejectOrderByOrderId(orderId, reason, userDetails);
	}

	@GetMapping("/operators")
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getAllOperators() {
		return operatorService.getAllOperators();
	}

	// Dashboard endpoints for admin
	@GetMapping("/orders/pending-payment")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getPendingPaymentOrders() {
		return orderService.getOrdersByStatus(OrderStatus.PENDING_PAYMENT);
	}

	@GetMapping("/orders/approved")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getApprovedOrders() {
		return orderService.getOrdersByStatus(OrderStatus.APPROVED);
	}

	@GetMapping("/orders/rejected")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getRejectedOrders() {
		return orderService.getOrdersByStatus(OrderStatus.REJECTED);
	}

	@GetMapping("/orders/cancelled")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getCancelledOrders() {
		return orderService.getOrdersByStatus(OrderStatus.CANCELLED);
	}
}