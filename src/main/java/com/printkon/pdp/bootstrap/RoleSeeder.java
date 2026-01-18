package com.printkon.pdp.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.printkon.pdp.common.enums.ERole;
import com.printkon.pdp.role.models.Role;
import com.printkon.pdp.role.repositories.RoleRepository;

@Component
public class RoleSeeder implements CommandLineRunner {

	@Autowired
	private RoleRepository roleRepository;

	@Override
	public void run(String... args) {
		for (ERole role : ERole.values()) {
			roleRepository.findByRole(role).orElseGet(() -> {
				Role newRole = Role.builder().role(role).build();
				roleRepository.save(newRole);
				System.out.println("Seeded role: " + role);
				return newRole;
			});
		}
	}
}
