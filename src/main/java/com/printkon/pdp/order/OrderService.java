package com.printkon.pdp.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.printkon.pdp.catalog.models.Product;
import com.printkon.pdp.catalog.repositories.ProductRepository;
import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.common.enums.ERole;
import com.printkon.pdp.common.enums.OrderStatus;
import com.printkon.pdp.common.enums.PaymentMethod;
import com.printkon.pdp.common.enums.PaymentStatus;
import com.printkon.pdp.exceptions.InvalidOperationException;
import com.printkon.pdp.exceptions.ResourceNotFoundException;
import com.printkon.pdp.order.dto.*;
import com.printkon.pdp.order.models.Order;
import com.printkon.pdp.order.models.OrderEvent;
import com.printkon.pdp.order.models.OrderItem;
import com.printkon.pdp.order.models.ShippingAddress;
import com.printkon.pdp.order.repositories.OrderEventRepository;
import com.printkon.pdp.order.repositories.OrderRepository;
import com.printkon.pdp.payment.PaymentService;
import com.printkon.pdp.user.UserDetailsImpl;
import com.printkon.pdp.user.models.Address;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.user.repositories.AddressRepository;
import com.printkon.pdp.user.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;
	private final OrderEventRepository orderEventRepository;
	private final UserRepository userRepository;
	private final AddressRepository addressRepository;
	private final PaymentService paymentService;

	// ========== ORDER PLACEMENT FLOW ==========

	@Transactional
	public ResponseEntity<ResponseStructure<OrderResponse>> placeOrder(OrderRequest request,
			UserDetailsImpl userDetails) {
		try {
			User user = userRepository.findById(userDetails.getId())
					.orElseThrow(() -> new RuntimeException("Authenticated user not found."));

			// Validation
			validateOrderRequest(request);

			// Build order components
			ShippingAddress shippingAddress = buildShippingAddressFromRequest(request, user);
			Order order = buildOrderFromRequest(request, user, shippingAddress);
			List<OrderItem> items = buildOrderItems(request, order);

			// Calculate total and set items
			BigDecimal computedTotal = calculateOrderTotal(items);
			order.setOrderTotal(computedTotal);
			order.setItems(items);

			// Set initial status based on payment method
			setInitialOrderStatus(order);

			// Save order
			Order savedOrder = orderRepository.save(order);
			createOrderEvent(savedOrder.getId(), "ORDER_CREATED", "Order placed successfully. Awaiting payment.",
					user.getId());

			log.info("Order {} placed by user {}", savedOrder.getOrderId(), user.getId());

			OrderResponse orderResponse = mapToOrderResponse(savedOrder);

			ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
					.message("Order placed successfully").data(orderResponse).statusCode(HttpStatus.CREATED.value())
					.build();

			return ResponseEntity.status(HttpStatus.CREATED).body(structure);

		} catch (Exception e) {
			log.error("Error placing order for user {}: {}", userDetails.getId(), e.getMessage());
			throw e;
		}
	}

	// ========== EXTERNAL-FACING METHODS (using orderId) ==========

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<UserOrderResponse>> getMyOrderByOrderId(String orderId,
			UserDetailsImpl userDetails) {
		Order order = orderRepository.findByOrderIdWithCustomer(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

		// Authorization check
		if (!order.getCustomer().getId().equals(userDetails.getId())) {
			throw new AccessDeniedException("Access denied to this order");
		}

		UserOrderResponse userOrderResponse = mapToUserOrderResponse(order);
		ResponseStructure<UserOrderResponse> structure = ResponseStructure.<UserOrderResponse>builder()
				.message("Order details fetched successfully").data(userOrderResponse).statusCode(HttpStatus.OK.value())
				.build();

		return ResponseEntity.ok(structure);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<UserOrderResponse>> cancelOrderByOrderId(String orderId,
			UserDetailsImpl userDetails, CancelOrderRequest cancelRequest) {

		try {
			log.info("Processing cancellation for order ID: {}, User ID: {}", orderId, userDetails.getId());

			// Validate cancellation request and extract reason
			String reason = validateAndGetCancellationReason(cancelRequest);

			// Find and validate order by orderId
			Order order = findAndValidateOrderForCancellationByOrderId(orderId, userDetails.getId());

			// Check if order can be cancelled based on business rules
			validateOrderCanBeCancelled(order);

			// Process cancellation with all business logic
			Order cancelledOrder = processOrderCancellation(order, reason, userDetails.getId());

			// Prepare response
			UserOrderResponse response = mapToUserOrderResponse(cancelledOrder);

			log.info("Order {} cancelled successfully by user {}", orderId, userDetails.getId());

			return ResponseEntity.ok(ResponseStructure.<UserOrderResponse>builder().statusCode(HttpStatus.OK.value())
					.message("Order cancelled successfully").data(response).build());

		} catch (ResourceNotFoundException | AccessDeniedException | InvalidOperationException e) {
			log.warn("Order cancellation failed: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Unexpected error during order cancellation: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to cancel order: " + e.getMessage());
		}
	}

	@Transactional
	public ResponseEntity<ResponseStructure<OrderResponse>> confirmPaymentByOrderId(
			PaymentConfirmationRequest request) {
		try {
			Order order = orderRepository.findByOrderId(request.getOrderId())
					.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

			// Verify payment
			boolean paymentVerified = verifyPayment(request);

			if (!paymentVerified) {
				return handlePaymentFailure(order, request);
			}

			// Update order status for successful payment
			order.setPaymentStatus(PaymentStatus.PAID);
			order.setStatus(OrderStatus.UNDER_REVIEW);
			order.setUpdatedAt(LocalDateTime.now());
			orderRepository.save(order);

			createOrderEvent(order.getId(), "PAYMENT_CONFIRMED", "Payment confirmed. Order moved to admin review.",
					null);

			log.info("Payment confirmed for order {}", order.getOrderId());

			ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
					.message("Payment confirmed successfully").data(mapToOrderResponse(order))
					.statusCode(HttpStatus.OK.value()).build();

			return ResponseEntity.ok(structure);

		} catch (Exception e) {
			log.error("Error confirming payment for order {}: {}", request.getOrderId(), e.getMessage());
			throw e;
		}
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<OrderResponse>> approveOrderByOrderId(String orderId,
			ApproveOrderRequest approveRequest, UserDetailsImpl currentUser) {

		Order order = orderRepository.findByOrderIdWithOperator(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

		// Validate order can be approved
		validateOrderForApproval(order);

		// Validate operator assignment
		User operator = validateAndGetOperator(approveRequest.getOperatorId());
		validateDeadline(approveRequest.getDeadline());

		User admin = userRepository.findById(currentUser.getId())
				.orElseThrow(() -> new RuntimeException("Admin user not found"));

		// Update order
		order.setStatus(OrderStatus.APPROVED);
		order.setAssignedOperator(operator);
		order.setReviewedBy(admin);
		order.setDeadline(approveRequest.getDeadline());
		order.setUpdatedAt(LocalDateTime.now());
		orderRepository.save(order);

		createOrderEvent(order.getId(), "ORDER_APPROVED",
				String.format("Order approved and assigned to operator %s. Deadline: %s", operator.getName(),
						approveRequest.getDeadline()),
				admin.getId());

		log.info("Order {} approved by admin {} and assigned to operator {}", orderId, admin.getId(), operator.getId());

		ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
				.message("Order approved and assigned to operator").data(mapToOrderResponse(order))
				.statusCode(HttpStatus.OK.value()).build();

		return ResponseEntity.ok(structure);
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<OrderResponse>> rejectOrderByOrderId(String orderId, String reason,
			UserDetailsImpl currentUser) {

		Order order = orderRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

		validateOrderForRejection(order);
		validateRejectionReason(reason);

		User admin = userRepository.findById(currentUser.getId())
				.orElseThrow(() -> new RuntimeException("Admin user not found"));

		order.setStatus(OrderStatus.REJECTED);
		order.setReviewedBy(admin);
		order.setRejectionReason(reason);
		order.setUpdatedAt(LocalDateTime.now());
		orderRepository.save(order);

		createOrderEvent(order.getId(), "ORDER_REJECTED", "Order rejected. Reason: " + reason, admin.getId());

		log.info("Order {} rejected by admin {}", orderId, admin.getId());

		ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
				.message("Order rejected successfully").data(mapToOrderResponse(order))
				.statusCode(HttpStatus.OK.value()).build();

		return ResponseEntity.ok(structure);
	}

	@Transactional
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<OrderResponse>> updateOrderStatusByOrderId(String orderId,
			OrderStatus newStatus, UserDetailsImpl userDetails, String notes) {

		Order order = orderRepository.findByOrderIdWithOperator(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

		// Validate operator assignment
		validateOperatorAssignment(order, userDetails.getId());

		// Validate status transition
		validateStatusTransition(order.getStatus(), newStatus);

		// Update order status
		OrderStatus oldStatus = order.getStatus();
		order.setStatus(newStatus);
		order.setUpdatedAt(LocalDateTime.now());
		orderRepository.save(order);

		String eventMessage = String.format("Status changed from %s to %s", oldStatus, newStatus);
		if (notes != null && !notes.trim().isEmpty()) {
			eventMessage += ". Notes: " + notes;
		}

		createOrderEvent(order.getId(), "STATUS_UPDATED", eventMessage, userDetails.getId());

		log.info("Order {} status updated from {} to {} by operator {}", orderId, oldStatus, newStatus,
				userDetails.getId());

		// If order is ready for delivery, create specific event
		if (newStatus == OrderStatus.READY_FOR_DELIVERY) {
			createOrderEvent(order.getId(), "ORDER_READY", "Order completed and ready for delivery",
					userDetails.getId());
		}

		ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
				.message("Order status updated successfully").data(mapToOrderResponse(order))
				.statusCode(HttpStatus.OK.value()).build();

		return ResponseEntity.ok(structure);
	}

	@Transactional
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderReadyForDeliveryByOrderId(String orderId,
			UserDetailsImpl userDetails, String preparationNotes) {

		return updateOrderStatusByOrderId(orderId, OrderStatus.READY_FOR_DELIVERY, userDetails, preparationNotes);
	}

	@Transactional
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderOutForDeliveryByOrderId(String orderId,
			UserDetailsImpl userDetails, String trackingNumber) {

		Order order = orderRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

		validateOperatorAssignment(order, userDetails.getId());

		if (order.getStatus() != OrderStatus.READY_FOR_DELIVERY) {
			throw new IllegalStateException("Order must be READY_FOR_DELIVERY before marking as OUT_FOR_DELIVERY");
		}

		order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
		if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
			order.setTrackingNumber(trackingNumber);
		}
		order.setUpdatedAt(LocalDateTime.now());
		orderRepository.save(order);

		createOrderEvent(order.getId(), "OUT_FOR_DELIVERY",
				"Order is out for delivery" + (trackingNumber != null ? ". Tracking: " + trackingNumber : ""),
				userDetails.getId());

		ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
				.message("Order marked as out for delivery").data(mapToOrderResponse(order))
				.statusCode(HttpStatus.OK.value()).build();

		return ResponseEntity.ok(structure);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<OrderResponse>> markOrderDeliveredByOrderId(String orderId) {
		Order order = orderRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

		if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
			throw new IllegalStateException("Order must be OUT_FOR_DELIVERY before marking as DELIVERED");
		}

		order.setStatus(OrderStatus.DELIVERED);
		order.setUpdatedAt(LocalDateTime.now());
		orderRepository.save(order);

		createOrderEvent(order.getId(), "ORDER_DELIVERED", "Order successfully delivered to customer", null);

		log.info("Order {} delivered successfully", orderId);

		ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
				.message("Order delivered successfully").data(mapToOrderResponse(order))
				.statusCode(HttpStatus.OK.value()).build();

		return ResponseEntity.ok(structure);
	}

	// ========== INTERNAL METHODS (using Long id) - Keep for backward compatibility
	// ==========

	@Transactional
	public ResponseEntity<ResponseStructure<OrderResponse>> confirmPayment(PaymentConfirmationRequest request) {
		// For backward compatibility - delegate to new method
		return confirmPaymentByOrderId(request);
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<OrderResponse>> approveOrder(Long orderId,
			ApproveOrderRequest approveRequest, UserDetailsImpl currentUser) {
		// Convert internal ID to orderId and delegate to new method
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
		return approveOrderByOrderId(order.getOrderId(), approveRequest, currentUser);
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<OrderResponse>> rejectOrder(Long orderId, String reason,
			UserDetailsImpl currentUser) {
		// Convert internal ID to orderId and delegate to new method
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
		return rejectOrderByOrderId(order.getOrderId(), reason, currentUser);
	}

	@Transactional
	@PreAuthorize("hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<OrderResponse>> updateOrderStatus(Long orderId, OrderStatus newStatus,
			UserDetailsImpl userDetails, String notes) {
		// Convert internal ID to orderId and delegate to new method
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
		return updateOrderStatusByOrderId(order.getOrderId(), newStatus, userDetails, notes);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<UserOrderResponse>> cancelOrder(Long orderId, UserDetailsImpl userDetails,
			CancelOrderRequest cancelRequest) {
		// Convert internal ID to orderId and delegate to new method
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
		return cancelOrderByOrderId(order.getOrderId(), userDetails, cancelRequest);
	}

	// ========== QUERY METHODS ==========

	public ResponseEntity<ResponseStructure<List<UserOrderResponse>>> getMyOrders(UserDetailsImpl userDetails) {
		User user = userRepository.findById(userDetails.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		List<Order> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(user);
		List<UserOrderResponse> responses = orders.stream().map(this::mapToUserOrderResponse)
				.collect(Collectors.toList());

		ResponseStructure<List<UserOrderResponse>> structure = ResponseStructure.<List<UserOrderResponse>>builder()
				.message("Orders fetched successfully").data(responses).statusCode(HttpStatus.OK.value()).build();

		return ResponseEntity.ok(structure);
	}

	// Updated to use orderId instead of internal id
	public ResponseEntity<ResponseStructure<?>> getOrderById(Long orderId, UserDetailsImpl userDetails) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

		// Authorization check
		boolean isOwner = order.getCustomer() != null && order.getCustomer().getId().equals(userDetails.getId());
		boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		boolean isOperator = userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR"));

		if (!isOwner && !isAdmin && !isOperator) {
			throw new AccessDeniedException("Access denied to this order");
		}

		// Use user-focused response for customers, admin response for admin/operator
		if (isOwner) {
			UserOrderResponse userOrderResponse = mapToUserOrderResponse(order);
			ResponseStructure<UserOrderResponse> structure = ResponseStructure.<UserOrderResponse>builder()
					.message("Order details fetched successfully").data(userOrderResponse)
					.statusCode(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(structure);
		} else {
			OrderResponse orderResponse = mapToOrderResponse(order);
			ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
					.message("Order details fetched successfully").data(orderResponse).statusCode(HttpStatus.OK.value())
					.build();
			return ResponseEntity.ok(structure);
		}
	}

	@PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getAllOrders(UserDetailsImpl userDetails,
			String status) {

		List<Order> orders;
		String message;

		User currentUser = userRepository.findById(userDetails.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		if (status != null && !status.isBlank()) {
			try {
				OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
				orders = isAdmin ? orderRepository.findByStatus(orderStatus)
						: orderRepository.findByStatusAndAssignedOperator(orderStatus, currentUser);
				message = "Fetched " + orders.size() + " orders with status " + status;
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Invalid order status: " + status);
			}
		} else {
			orders = isAdmin ? orderRepository.findAll() : orderRepository.findByAssignedOperator(currentUser);
			message = "Fetched all orders";
		}

		List<OrderResponse> responses = orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());

		ResponseStructure<List<OrderResponse>> structure = ResponseStructure.<List<OrderResponse>>builder()
				.message(message).data(responses).statusCode(HttpStatus.OK.value()).build();

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersByStatus(OrderStatus status) {
		List<Order> orders = orderRepository.findByStatus(status);
		List<OrderResponse> responses = orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());

		ResponseStructure<List<OrderResponse>> structure = ResponseStructure.<List<OrderResponse>>builder()
				.message("Fetched " + responses.size() + " orders with status " + status).data(responses)
				.statusCode(HttpStatus.OK.value()).build();

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersUnderReview() {
		return getOrdersByStatus(OrderStatus.UNDER_REVIEW);
	}

	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersInProduction() {
		return getOrdersByStatus(OrderStatus.IN_PRODUCTION);
	}

	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersReadyForDelivery() {
		return getOrdersByStatus(OrderStatus.READY_FOR_DELIVERY);
	}

	public ResponseEntity<ResponseStructure<List<OrderResponse>>> getOrdersAssignedToOperator(
			UserDetailsImpl userDetails) {
		List<Order> orders = orderRepository.findByAssignedOperatorId(userDetails.getId());
		List<OrderResponse> responses = orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());

		ResponseStructure<List<OrderResponse>> structure = ResponseStructure.<List<OrderResponse>>builder()
				.message("Assigned orders fetched successfully").data(responses).statusCode(HttpStatus.OK.value())
				.build();

		return ResponseEntity.ok(structure);
	}

	// ========== CANCELLATION BUSINESS LOGIC METHODS ==========

	private String validateAndGetCancellationReason(CancelOrderRequest cancelRequest) {
		if (cancelRequest == null) {
			throw new IllegalArgumentException("Cancellation request cannot be null");
		}

		String reason = cancelRequest.getReason() != null ? cancelRequest.getReason().trim() : "";

		if (reason.isEmpty()) {
			reason = "No reason provided";
		}

		if (reason.length() > 500) {
			throw new IllegalArgumentException("Cancellation reason must not exceed 500 characters");
		}

		return reason;
	}

	private Order findAndValidateOrderForCancellationByOrderId(String orderId, Long userId) {
		Order order = orderRepository.findByOrderIdWithCustomer(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

		// Validate user ownership
		if (!order.getCustomer().getId().equals(userId)) {
			throw new AccessDeniedException("You are not authorized to cancel this order");
		}

		return order;
	}

	@SuppressWarnings("unused")
	private Order findAndValidateOrderForCancellation(Long orderId, Long userId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

		// Validate user ownership
		if (!order.getCustomer().getId().equals(userId)) {
			throw new AccessDeniedException("You are not authorized to cancel this order");
		}

		return order;
	}

	private void validateOrderCanBeCancelled(Order order) {
		// Define cancellable statuses based on business rules
		List<OrderStatus> cancellableStatuses = Arrays.asList(OrderStatus.PENDING_PAYMENT, OrderStatus.UNDER_REVIEW,
				OrderStatus.APPROVED, OrderStatus.PROCESSING);

		if (!cancellableStatuses.contains(order.getStatus())) {
			throw new InvalidOperationException(String.format(
					"Order cannot be cancelled in its current status: %s. "
							+ "Only orders with status: %s can be cancelled.",
					order.getStatus(), getReadableStatusNames(cancellableStatuses)));
		}

		// Additional business validations
		if (order.getCancelledAt() != null) {
			throw new InvalidOperationException("Order is already cancelled");
		}

		// Validate order is not too far in production process
		if (order.getStatus() == OrderStatus.IN_PRODUCTION) {
			validateInProductionOrderCancellation(order);
		}
	}

	private List<String> getReadableStatusNames(List<OrderStatus> statuses) {
		return statuses.stream().map(status -> status.toString().toLowerCase().replace("_", " "))
				.collect(Collectors.toList());
	}

	private void validateInProductionOrderCancellation(Order order) {
		// Business rule: Orders in production for more than 2 hours cannot be cancelled
		LocalDateTime productionStartTime = getProductionStartTime(order);
		if (productionStartTime != null) {
			long hoursInProduction = java.time.Duration.between(productionStartTime, LocalDateTime.now()).toHours();
			if (hoursInProduction > 2) {
				throw new InvalidOperationException(
						"Order cannot be cancelled as it has been in production for more than 2 hours");
			}
		}
	}

	private LocalDateTime getProductionStartTime(Order order) {
		// Find when the order entered IN_PRODUCTION status
		return orderEventRepository
				.findFirstByOrderIdAndEventTypeOrderByCreatedAtAsc(order.getId(), "STATUS_UPDATED_TO_IN_PRODUCTION")
				.map(OrderEvent::getCreatedAt).orElse(order.getUpdatedAt());
	}

	private Order processOrderCancellation(Order order, String reason, Long userId) {
		// Store original status for event tracking
		OrderStatus originalStatus = order.getStatus();

		// Update order status and cancellation details
		order.setStatus(OrderStatus.CANCELLED);
		order.setCancellationReason(reason);
		order.setCancelledByUserId(userId);
		order.setCancelledAt(LocalDateTime.now());
		order.setUpdatedAt(LocalDateTime.now());

		// Add comprehensive order event for tracking
		addCancellationOrderEvent(order, originalStatus, reason, userId);

		// Handle payment refund if applicable
		handlePaymentRefundForCancellation(order);

		// Notify relevant parties (admin, operator if assigned)
		notifyPartiesAboutCancellation(order, originalStatus);

		return orderRepository.save(order);
	}

	private void addCancellationOrderEvent(Order order, OrderStatus originalStatus, String reason, Long userId) {
		String eventDescription = String.format("Order cancelled by user. Original status: %s. Reason: %s",
				originalStatus, reason);

		OrderEvent event = OrderEvent.builder().orderId(order.getId()).eventType("ORDER_CANCELLED")
				.message(eventDescription).performedByUserId(userId).createdAt(LocalDateTime.now()).build();

		orderEventRepository.save(event);
	}

	private void handlePaymentRefundForCancellation(Order order) {
		// Initiate refund process for paid orders
		if (order.getPaymentStatus() == PaymentStatus.PAID) {
			log.info("Initiating refund process for cancelled order: {}", order.getId());

			try {
				// Update payment status to reflect refund process
				order.setPaymentStatus(PaymentStatus.REFUND_PENDING);

				// Add refund initiation event
				OrderEvent refundEvent = OrderEvent.builder().orderId(order.getId()).eventType("REFUND_INITIATED")
						.message("Refund process initiated due to order cancellation")
						.performedByUserId(order.getCancelledByUserId()).createdAt(LocalDateTime.now()).build();
				orderEventRepository.save(refundEvent);

				// TODO: Integrate with actual payment gateway refund API
				// paymentService.initiateRefund(order.getId(), order.getOrderTotal());

			} catch (Exception e) {
				log.error("Failed to initiate refund for order {}: {}", order.getId(), e.getMessage());
				// Don't fail the cancellation if refund initiation fails
				OrderEvent refundErrorEvent = OrderEvent.builder().orderId(order.getId())
						.eventType("REFUND_INITIATION_FAILED").message("Failed to initiate refund: " + e.getMessage())
						.performedByUserId(order.getCancelledByUserId()).createdAt(LocalDateTime.now()).build();
				orderEventRepository.save(refundErrorEvent);
			}
		} else if (order.getPaymentStatus() == PaymentStatus.PENDING && order.getPaymentMethod() != PaymentMethod.COD) {
			// For pending online payments, just update status
			order.setPaymentStatus(PaymentStatus.CANCELLED);
		}
	}

	private void notifyPartiesAboutCancellation(Order order, OrderStatus originalStatus) {
		try {
			// Notify admin if order was under review or approved
			if (originalStatus == OrderStatus.UNDER_REVIEW || originalStatus == OrderStatus.APPROVED) {
				notifyAdminAboutCancellation(order);
			}

			// Notify operator if order was assigned
			if (order.getAssignedOperator() != null) {
				notifyOperatorAboutCancellation(order);
			}

			log.info("Cancellation notifications sent for order: {}", order.getId());

		} catch (Exception e) {
			log.warn("Failed to send cancellation notifications for order {}: {}", order.getId(), e.getMessage());
			// Don't fail cancellation if notifications fail
		}
	}

	private void notifyAdminAboutCancellation(Order order) {
		// TODO: Implement admin notification (email, in-app, etc.)
		log.info("Admin should be notified about cancellation of order: {}", order.getId());

		OrderEvent notificationEvent = OrderEvent.builder().orderId(order.getId()).eventType("ADMIN_NOTIFIED")
				.message("Admin notified about order cancellation").createdAt(LocalDateTime.now()).build();
		orderEventRepository.save(notificationEvent);
	}

	private void notifyOperatorAboutCancellation(Order order) {
		// TODO: Implement operator notification
		log.info("Operator {} should be notified about cancellation of order: {}", order.getAssignedOperator().getId(),
				order.getId());

		OrderEvent notificationEvent = OrderEvent.builder().orderId(order.getId()).eventType("OPERATOR_NOTIFIED")
				.message("Assigned operator notified about order cancellation").createdAt(LocalDateTime.now()).build();
		orderEventRepository.save(notificationEvent);
	}

	// ========== VALIDATION METHODS ==========

	private void validateOrderRequest(OrderRequest request) {
		if (request.getItems() == null || request.getItems().isEmpty()) {
			throw new IllegalArgumentException("Order must contain at least one item");
		}
		if (request.getPaymentMethod() == null) {
			throw new IllegalArgumentException("Payment method must be specified");
		}
	}

	private void validateOrderForApproval(Order order) {
		if (order.getStatus() != OrderStatus.UNDER_REVIEW) {
			throw new IllegalStateException("Only orders under review can be approved");
		}
		if (order.getPaymentStatus() != PaymentStatus.PAID && order.getPaymentMethod() != PaymentMethod.COD) {
			throw new IllegalStateException("Only paid orders can be approved");
		}
	}

	private void validateOrderForRejection(Order order) {
		if (order.getStatus() != OrderStatus.UNDER_REVIEW) {
			throw new IllegalStateException("Only orders under review can be rejected");
		}
	}

	private void validateRejectionReason(String reason) {
		if (reason == null || reason.trim().isEmpty()) {
			throw new IllegalArgumentException("Rejection reason must be provided");
		}
	}

	private void validateOperatorAssignment(Order order, Long operatorId) {
		if (order.getAssignedOperator() == null) {
			throw new AccessDeniedException("Order is not assigned to any operator");
		}
		if (!order.getAssignedOperator().getId().equals(operatorId)) {
			throw new AccessDeniedException("You are not assigned to this order");
		}
	}

	private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
		// Define allowed transitions
		switch (currentStatus) {
		case APPROVED:
			if (newStatus != OrderStatus.IN_PRODUCTION && newStatus != OrderStatus.CANCELLED) {
				throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
			}
			break;
		case IN_PRODUCTION:
			if (newStatus != OrderStatus.READY_FOR_DELIVERY && newStatus != OrderStatus.CANCELLED) {
				throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
			}
			break;
		case READY_FOR_DELIVERY:
			if (newStatus != OrderStatus.OUT_FOR_DELIVERY && newStatus != OrderStatus.CANCELLED) {
				throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
			}
			break;
		case OUT_FOR_DELIVERY:
			if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
				throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
			}
			break;
		default:
			throw new IllegalStateException("Cannot transition from " + currentStatus + " to " + newStatus);
		}
	}

	private User validateAndGetOperator(Long operatorId) {
		User operator = userRepository.findById(operatorId)
				.orElseThrow(() -> new RuntimeException("Operator not found"));

		boolean isOperator = operator.getRoles().stream().anyMatch(role -> role.getRole() == ERole.OPERATOR);

		if (!isOperator) {
			throw new RuntimeException("Assigned user must have OPERATOR role");
		}

		return operator;
	}

	private void validateDeadline(LocalDate deadline) {
		if (deadline == null || deadline.isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("Valid deadline must be provided (future date)");
		}
	}

	// ========== HELPER METHODS ==========

	private boolean verifyPayment(PaymentConfirmationRequest request) {
		try {
			return paymentService.verifyPayment(request.getRawPayload(), request.getProviderPaymentId());
		} catch (Exception e) {
			log.error("Payment verification failed: {}", e.getMessage());
			return false;
		}
	}

	private ResponseEntity<ResponseStructure<OrderResponse>> handlePaymentFailure(Order order,
			PaymentConfirmationRequest request) {
		order.setPaymentStatus(PaymentStatus.FAILED);
		order.setStatus(OrderStatus.CANCELLED);
		order.setUpdatedAt(LocalDateTime.now());
		orderRepository.save(order);

		createOrderEvent(order.getId(), "PAYMENT_FAILED", "Payment verification failed. Order cancelled.", null);

		ResponseStructure<OrderResponse> structure = ResponseStructure.<OrderResponse>builder()
				.message("Payment verification failed").data(mapToOrderResponse(order))
				.statusCode(HttpStatus.BAD_REQUEST.value()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(structure);
	}

	private void setInitialOrderStatus(Order order) {
		if (order.getPaymentMethod() == PaymentMethod.COD) {
			order.setStatus(OrderStatus.UNDER_REVIEW); // COD orders go directly to review
			order.setPaymentStatus(PaymentStatus.PENDING);
		} else {
			order.setStatus(OrderStatus.PENDING_PAYMENT);
			order.setPaymentStatus(PaymentStatus.PENDING);
		}
	}

	private List<OrderItem> buildOrderItems(OrderRequest request, Order order) {
		return request.getItems().stream().map(itemReq -> mapToOrderItem(itemReq, order)).collect(Collectors.toList());
	}

	private BigDecimal calculateOrderTotal(List<OrderItem> items) {
		return items.stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ========== ENHANCED MAPPING METHODS ==========

	private ShippingAddress buildShippingAddressFromRequest(OrderRequest request, User user) {
		if (request.getShippingAddress() != null) {
			return request.getShippingAddress();
		}

		if (request.getShippingAddressId() != null) {
			Address saved = addressRepository.findById(request.getShippingAddressId())
					.orElseThrow(() -> new RuntimeException("Address not found"));

			if (!saved.getUser().getId().equals(user.getId())) {
				throw new AccessDeniedException("Address does not belong to user");
			}

			return ShippingAddress.builder().recipientName(saved.getRecipientName()).phone(saved.getPhone())
					.addressLine(saved.getAddressLine()).city(saved.getCity()).state(saved.getState())
					.zip(saved.getZip()).country(saved.getCountry()).build();
		}

		throw new IllegalArgumentException("Shipping address must be provided");
	}

	private Order buildOrderFromRequest(OrderRequest request, User user, ShippingAddress shippingAddress) {
		return Order.builder().createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
				.paymentMethod(request.getPaymentMethod()).shippingAddress(shippingAddress)
				.deliveryInstructions(request.getDeliveryInstructions()).customer(user).build();
	}

	private OrderItem mapToOrderItem(OrderItemRequest itemRequest, Order order) {
		Product product = productRepository.findByProductId(itemRequest.getProductId())
				.orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));

		BigDecimal unitPrice = product.getPrice();
		int qty = itemRequest.getQuantity() != null ? itemRequest.getQuantity() : 1;
		BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

		return OrderItem.builder().product(product).quantity(qty).size(itemRequest.getSize())
				.customNote(itemRequest.getCustomNote()).unitPrice(unitPrice).totalPrice(totalPrice).order(order)
				.build();
	}

	// UPDATED: Use orderId in responses
	private UserOrderResponse mapToUserOrderResponse(Order order) {
		return UserOrderResponse.builder().orderId(order.getOrderId()) // Use orderId instead of id
				.status(order.getStatus().toString()).createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt())
				.orderTotal(order.getOrderTotal())
				.paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : null)
				.paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : null)
				.shippingAddress(order.getShippingAddress()).deliveryInstructions(order.getDeliveryInstructions())
				.estimatedDeliveryDate(order.getEstimatedDeliveryDate()).trackingNumber(order.getTrackingNumber())
				.cancellationReason(order.getCancellationReason()).orderEvents(getOrderEventsForUser(order.getId()))
				.items(order.getItems() != null
						? order.getItems().stream().map(this::mapToUserOrderItemResponse).collect(Collectors.toList())
						: Collections.emptyList())
				.build();
	}

	private UserOrderItemResponse mapToUserOrderItemResponse(OrderItem item) {
		Product product = item.getProduct();
		return UserOrderItemResponse.builder().productId(product.getId()).productName(product.getName())
				.productDescription(product.getDescription()).mainImageUrl(product.getMainImageUrl())
				.imageUrls(product.getImageUrls()).quantity(item.getQuantity()).size(item.getSize())
				.customNote(item.getCustomNote()).unitPrice(item.getUnitPrice()).totalPrice(item.getTotalPrice())
				.categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
				.categoryId(product.getCategory() != null ? product.getCategory().getId() : null).build();
	}

	private List<OrderEventResponse> getOrderEventsForUser(Long orderId) {
		return orderEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
				.map(event -> OrderEventResponse.builder().eventType(event.getEventType()).message(event.getMessage())
						.createdAt(event.getCreatedAt()).performedBy(getUserName(event.getPerformedByUserId())).build())
				.collect(Collectors.toList());
	}

	private String getUserName(Long userId) {
		if (userId == null)
			return "System";
		return userRepository.findById(userId).map(User::getName).orElse("Unknown User");
	}

	// UPDATED: Use orderId in admin responses
	private OrderResponse mapToOrderResponse(Order order) {
		return OrderResponse.builder().orderId(order.getOrderId()) // Use orderId instead of id
				.status(order.getStatus().toString()).createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt())
				.orderTotal(order.getOrderTotal()).paymentMethod(order.getPaymentMethod())
				.paymentStatus(order.getPaymentStatus()).shippingAddress(order.getShippingAddress())
				.deliveryInstructions(order.getDeliveryInstructions())
				.estimatedDeliveryDate(order.getEstimatedDeliveryDate()).trackingNumber(order.getTrackingNumber())
				.rejectionReason(order.getRejectionReason())
				.customerName(order.getCustomer() != null ? order.getCustomer().getName() : null)
				.assignedOperatorName(
						order.getAssignedOperator() != null ? order.getAssignedOperator().getName() : null)
				.approvedByName(order.getReviewedBy() != null ? order.getReviewedBy().getName() : null)
				.items(order.getItems() != null
						? order.getItems().stream().map(this::mapToOrderItemResponse).collect(Collectors.toList())
						: null)
				.build();
	}

	private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
		return OrderItemResponse.builder().productName(item.getProduct() != null ? item.getProduct().getName() : null)
				.quantity(item.getQuantity()).size(item.getSize()).customNote(item.getCustomNote())
				.unitPrice(item.getUnitPrice()).totalPrice(item.getTotalPrice()).build();
	}

	private void createOrderEvent(Long orderId, String type, String message, Long performedBy) {
		OrderEvent event = OrderEvent.builder().orderId(orderId).eventType(type).message(message)
				.performedByUserId(performedBy).createdAt(LocalDateTime.now()).build();
		orderEventRepository.save(event);
	}
}