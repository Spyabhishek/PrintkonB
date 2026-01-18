package com.printkon.pdp.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonialRequest {

	/**
	 * The author / customer name (optional but useful)
	 */
	private String author;

	/**
	 * e.g. "Founder, ACME" or "Verified Buyer"
	 */
	private String role;

	@NotBlank(message = "Quote is required")
	private String quote;

	/**
	 * Optional image URL for author avatar
	 */
	private String imageUrl;

	private Integer orderIndex;
	private Boolean enabled;
}
