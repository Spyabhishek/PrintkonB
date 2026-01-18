package com.printkon.pdp.shopping.cart.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.shopping.cart.models.Cart;
import com.printkon.pdp.user.models.User;

public interface CartRepository extends JpaRepository<Cart, Long> {
	Optional<Cart> findByUser(User user);

	Optional<Cart> findByUserId(Long userId);
}
