package com.printkon.pdp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

	@NotBlank(message = "Current password is required for verification")
	private String currentPassword;

	@Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
	private String name;

	@Email(message = "Please provide a valid email address")
	private String email;

	@Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters")
	private String phone;

	private String gender;

	@Min(value = 1, message = "Age must be at least 1")
	private Integer age;

	@Size(min = 6, message = "New password must be at least 6 characters long")
	private String newPassword;
}
