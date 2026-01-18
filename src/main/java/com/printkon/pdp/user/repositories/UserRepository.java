package com.printkon.pdp.user.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.printkon.pdp.common.enums.AccountStatus;
import com.printkon.pdp.common.enums.ERole;
import com.printkon.pdp.user.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	// Existing methods
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByPhone(String phone);

	List<User> findByNameContainingIgnoreCase(String name);

	List<User> findByAccountStatus(AccountStatus status);

	@Query("SELECT u FROM User u WHERE u.phone = :phone")
	Optional<User> findByPhone(@Param("phone") String phone);

	@Query("SELECT u FROM User u WHERE u.name LIKE %:name% AND u.accountStatus = :status")
	List<User> findByNameContainingAndAccountStatus(@Param("name") String name, @Param("status") AccountStatus status);

	@Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus = :status")
	long countByAccountStatus(@Param("status") AccountStatus status);

	// Role-based queries using JOIN
	@Query("SELECT u FROM User u JOIN u.roles r WHERE r.role = :role")
	List<User> findByRole(@Param("role") ERole role);

	@Query("SELECT u FROM User u JOIN u.roles r WHERE r.role = :role AND u.accountStatus = :status")
	List<User> findByRoleAndAccountStatus(@Param("role") ERole role, @Param("status") AccountStatus status);

	@Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.role = :role")
	long countByRole(@Param("role") ERole role);

	// Check if user has specific role
	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u JOIN u.roles r WHERE u.email = :email AND r.role = :role")
	boolean existsByEmailAndRole(@Param("email") String email, @Param("role") ERole role);

	// Get users with multiple roles
	@Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.role IN :roles")
	List<User> findByRolesIn(@Param("roles") List<ERole> roles);
}