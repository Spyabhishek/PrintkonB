package com.printkon.pdp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, max = 40, message = "Password length must be between 8 and 40")
    private String password;

    // optional: client-generated UUID stored in localStorage
    private String deviceId;

    // optional: navigator.userAgent (truncate on server)
    @Size(max = 512)
    private String userAgent;
}
