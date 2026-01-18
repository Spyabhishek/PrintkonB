package com.printkon.pdp.order.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class UserOrderItemResponse {
	private Long productId;
	private String productName;
	private String productDescription;
	private String mainImageUrl;
	private List<String> imageUrls;
	private Integer quantity;
	private String size;
	private String customNote;
	private BigDecimal unitPrice;
	private BigDecimal totalPrice;
	private String categoryName;
	private Long categoryId;
}