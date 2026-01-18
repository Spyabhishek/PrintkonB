package com.printkon.pdp.shopping.wishlist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WishlistItemRequest {
	@NotNull
	private Long productId;
}