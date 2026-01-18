package com.printkon.pdp.user;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.printkon.pdp.auth.dto.PasswordConfirmationRequest;
import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.common.enums.AccountStatus;
import com.printkon.pdp.common.enums.ERole;
import com.printkon.pdp.user.dto.UserResponse;
import com.printkon.pdp.user.dto.UserUpdateRequest;
import com.printkon.pdp.user.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// === READ OPERATIONS ===
	@GetMapping("/current")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ResponseStructure<UserResponse>> getCurrentUser() {
		return userService.getCurrentUser();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN') or (#id == authentication.principal.id)")
	public ResponseEntity<ResponseStructure<UserResponse>> getUserById(@PathVariable Long id) {
		return userService.getUserById(id);
	}

	@GetMapping
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getAllUsers() {
		return userService.getAllUsers();
	}

	@GetMapping("/email/{email}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<UserResponse>> getUserByEmail(@PathVariable String email) {
		return userService.getUserByEmail(email);
	}

	@GetMapping("/search")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<List<UserResponse>>> searchUsersByName(@RequestParam String name) {
		return userService.searchUsersByName(name);
	}

	@GetMapping("/status/{status}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getUsersByAccountStatus(
			@PathVariable AccountStatus status) {
		return userService.getUsersByAccountStatus(status);
	}

	@GetMapping("/role/{role}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getUsersByRole(@PathVariable ERole role) {
		return userService.getUsersByRole(role);
	}

	@GetMapping("/role/{role}/status/{status}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getUsersByRoleAndStatus(@PathVariable ERole role,
			@PathVariable AccountStatus status) {
		return userService.getUsersByRoleAndStatus(role, status);
	}

	@GetMapping("/count")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<Long>> getUserCount() {
		return userService.getUserCount();
	}

	@GetMapping("/count/role/{role}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<Long>> getUserCountByRole(@PathVariable ERole role) {
		return userService.getUserCountByRole(role);
	}

	// === UPDATE OPERATIONS ===

	@PatchMapping("/update-current/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN') or (#id == authentication.principal.id)")
	public ResponseEntity<ResponseStructure<UserResponse>> updateUser(@PathVariable Long id,
			@Valid @RequestBody UserUpdateRequest updateRequest) {
		return userService.updateUser(id, updateRequest);
	}

	@PatchMapping("/update-current")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ResponseStructure<UserResponse>> updateCurrentUser(
			@Valid @RequestBody UserUpdateRequest updateRequest) {
		return userService.updateCurrentUser(updateRequest);
	}

	// === DELETE OPERATIONS ===

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<String>> deleteUser(@PathVariable Long id) {
		return userService.deleteUser(id);
	}

	@DeleteMapping("/current")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ResponseStructure<String>> deleteCurrentUser(
			@Valid @RequestBody PasswordConfirmationRequest passwordRequest) {
		return userService.deleteCurrentUser(passwordRequest.getCurrentPassword());
	}

	// Alternative delete current user with query parameter
	@DeleteMapping("/current/confirm")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ResponseStructure<String>> deleteCurrentUserWithPassword(
			@RequestParam String currentPassword) {
		return userService.deleteCurrentUser(currentPassword);
	}

	// === UTILITY ENDPOINTS ===

	@GetMapping("/exists/email/{email}")
	public ResponseEntity<ResponseStructure<Boolean>> checkEmailExists(@PathVariable String email) {
		boolean exists = userService.existsByEmail(email);
		ResponseStructure<Boolean> structure = new ResponseStructure<>();
		structure.setStatusCode(200);
		structure.setMessage(exists ? "Email already exists" : "Email is available");
		structure.setData(exists);
		return ResponseEntity.ok(structure);
	}

	@GetMapping("/exists/phone/{phone}")
	public ResponseEntity<ResponseStructure<Boolean>> checkPhoneExists(@PathVariable String phone) {
		boolean exists = userService.existsByPhone(phone);
		ResponseStructure<Boolean> structure = new ResponseStructure<>();
		structure.setStatusCode(200);
		structure.setMessage(exists ? "Phone number already exists" : "Phone number is available");
		structure.setData(exists);
		return ResponseEntity.ok(structure);
	}
}