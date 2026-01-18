package com.printkon.pdp.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

	@NotBlank(message = "Name is mandatory")
	private String name;

	@Email(message = "Invalid email format")
	private String email;

	@NotBlank
	@Size(min = 10, max = 15)
	private Long phone;
	
	@NotNull
	@Min(18)
	@NotBlank(message = "Age is mandatory")
	private int age;

	@NotBlank(message = "Gender is mandatory")
	private String gender;

	@NotBlank(message = "Password is mandatory")
	@Size(min = 8, max = 40, message = "Password length must be between 8 to 15")
	private String password;
}
