package com.printkon.pdp.shopping.wishlist.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WishlistItemResponse {
	private Long id;
	private String productId;
	private String productName;
	private BigDecimal productPrice;
	private String imageUrl;
	private Boolean available;
	private Boolean inStock;
	private String categoryName;
	private Double averageRating;
	private Long reviewCount;
}