package com.printkon.pdp.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrintingServiceRequest {
	@NotBlank(message = "Title is required")
	private String title;
	private String description;
	private String iconUrl;
	private Integer orderIndex;
	private Boolean enabled;
}
