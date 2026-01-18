package com.printkon.pdp.misc;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verify")
public class Hello {
	
	@GetMapping("/greet")
	@PreAuthorize("hasRole('USER')")
	public String greet() {
		return "welcome to the world of Darkness..!";
	}
}
