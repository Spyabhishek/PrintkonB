package com.printkon.pdp.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatRequest {
	@NotBlank(message = "Label is required")
	private String label;

	@NotBlank(message = "Value is required")
	private String value;

	/**
	 * If true, the stat will be treated as dynamic (e.g. derived from DB counts)
	 */
	private Boolean isDynamic;

	private Integer orderIndex;
	private Boolean enabled;
}
