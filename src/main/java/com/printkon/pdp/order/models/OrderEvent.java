package com.printkon.pdp.order.models;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long orderId;

	private String eventType; // e.g., "STATUS_CHANGED", "PAYMENT_CONFIRMED", "CANCELLED"
	private String message;

	private LocalDateTime createdAt;

	private Long performedByUserId; // optional
}
