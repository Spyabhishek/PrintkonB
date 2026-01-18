package com.printkon.pdp.order.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import com.printkon.pdp.common.enums.OrderStatus;
import com.printkon.pdp.common.enums.PaymentMethod;
import com.printkon.pdp.common.enums.PaymentStatus;
import com.printkon.pdp.user.models.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // Internal use only

	@Column(name = "order_id", unique = true, nullable = false, length = 20, updatable = false)
	private String orderId; // External/public facing

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 30)
	private OrderStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", length = 20)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", length = 20)
	private PaymentStatus paymentStatus;

	private BigDecimal orderTotal;

	@Embedded
	private ShippingAddress shippingAddress;

	@Column(length = 500)
	private String deliveryInstructions;

	@Column(length = 100)
	private String trackingNumber;

	private LocalDate estimatedDeliveryDate;

	@ManyToOne
	@JoinColumn(name = "customer_id", nullable = false)
	private User customer;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items;

	@ManyToOne
	@JoinColumn(name = "assigned_operator_id")
	private User assignedOperator;

	@ManyToOne
	@JoinColumn(name = "reviewed_by_id")
	private User reviewedBy;

	private LocalDate deadline;

	@Column(length = 500)
	private String rejectionReason;

	@Column(length = 500)
	private String cancellationReason;

	@Column(name = "cancelled_by_user_id")
	private Long cancelledByUserId;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@PrePersist
	protected void generateOrderId() {
		if (this.orderId == null) {
			String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
			String randomPart = String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 9999));
			this.orderId = "ORD-" + datePart + "-" + randomPart;
		}
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}