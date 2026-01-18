package com.printkon.pdp.order.models;

import java.math.BigDecimal;

import com.printkon.pdp.catalog.models.Product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer quantity;
	private String size;
	private String customNote;

	// Snapshot of product price at order time for immutability
	private BigDecimal unitPrice;
	private BigDecimal totalPrice;

	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	private Order order;
}
