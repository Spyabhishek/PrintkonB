package com.printkon.pdp.review.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewResponse {
	private Long id;
	private Long productId;
	private Long userId;
	private Integer rating;
	private String title;
	private String comment;
	private Boolean approved;
	private LocalDateTime createdAt;
}
