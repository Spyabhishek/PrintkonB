package com.printkon.pdp.cms.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialResponse {
	private Long id;
	private String author;
	private String role;
	private String quote;
	private String imageUrl;
	private Boolean enabled;
	private Integer orderIndex;
}
