package com.printkon.pdp.review.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewRequest {
	@NotNull
	@Min(1)
	@Max(5)
	private Integer rating;

	private String title;

	private String comment;
}
