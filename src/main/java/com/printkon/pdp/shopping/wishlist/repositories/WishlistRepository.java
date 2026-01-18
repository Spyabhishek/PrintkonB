package com.printkon.pdp.shopping.wishlist.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.shopping.wishlist.models.Wishlist;
import com.printkon.pdp.user.models.User;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
	Optional<Wishlist> findByUser(User user);

	Optional<Wishlist> findByUserId(Long userId);
}
