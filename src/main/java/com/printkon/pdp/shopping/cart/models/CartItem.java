package com.printkon.pdp.shopping.cart.models;


import com.printkon.pdp.catalog.models.Product;
import com.printkon.pdp.common.jpa.JsonAttributeConverter;

import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;

    // product snapshot reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // flexible options: paper_type, size, color, uploaded_design_path, customNotes, etc.
    @Convert(converter = JsonAttributeConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> options;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;
}
