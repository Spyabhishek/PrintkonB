package com.printkon.pdp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
	@NotBlank(message = "Name is required")
	private String name;

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String email;

	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = "^[0-9]\\d{9}$", message = "Phone number must be exactly 10 digits")
	private String phone;

	@Min(value = 13, message = "Age must be at least 13")
	@Max(value = 100, message = "Age must be less than 120")
	private int age;

	@NotBlank(message = "Gender is mandatory")
	@Pattern(regexp = "Male|Female|Other", message = "Gender must be Male, Female, or Other")
	private String gender;

	@NotBlank(message = "Password is required")
	@Size(min = 6, message = "Password must be at least 6 characters")
	private String password;
}
