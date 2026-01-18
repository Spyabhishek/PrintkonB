package com.printkon.pdp.shopping.wishlist.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.shopping.wishlist.models.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
}
