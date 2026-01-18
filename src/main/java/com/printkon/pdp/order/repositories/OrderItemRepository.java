package com.printkon.pdp.order.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.order.models.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}