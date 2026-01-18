package com.printkon.pdp.role.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.common.enums.ERole;
import com.printkon.pdp.role.models.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> findByRole(ERole role);
}
