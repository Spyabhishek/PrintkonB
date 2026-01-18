package com.printkon.pdp.shopping.cart.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.shopping.cart.models.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}