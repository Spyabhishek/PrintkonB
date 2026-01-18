package com.printkon.pdp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

	@NotBlank(message = "Label is required")
	@Size(max = 50, message = "Label must not exceed 50 characters")
	private String label;

	@NotBlank(message = "Recipient name is required")
	@Size(max = 100, message = "Recipient name must not exceed 100 characters")
	private String recipientName;

	@NotBlank(message = "Phone number is required")
	@Size(max = 20, message = "Phone number must not exceed 20 characters")
	private String phone;

	@NotBlank(message = "Address line is required")
	@Size(max = 255, message = "Address line must not exceed 255 characters")
	private String addressLine;

	@NotBlank(message = "City is required")
	@Size(max = 100, message = "City must not exceed 100 characters")
	private String city;

	@NotBlank(message = "State is required")
	@Size(max = 100, message = "State must not exceed 100 characters")
	private String state;

	@NotBlank(message = "ZIP code is required")
	@Size(max = 20, message = "ZIP code must not exceed 20 characters")
	private String zip;

	@NotBlank(message = "Country is required")
	@Size(max = 100, message = "Country must not exceed 100 characters")
	private String country;

	@NotNull(message = "Default flag is required")
	private Boolean isDefault;
}