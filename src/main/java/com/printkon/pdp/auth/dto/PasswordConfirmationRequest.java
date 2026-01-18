package com.printkon.pdp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordConfirmationRequest {

	@NotBlank(message = "Current password is required for confirmation")
	private String currentPassword;
}
