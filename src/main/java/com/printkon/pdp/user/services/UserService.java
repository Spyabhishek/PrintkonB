package com.printkon.pdp.user.services;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.printkon.pdp.auth.dto.ForgotPasswordRequest;
import com.printkon.pdp.auth.dto.ResetPasswordRequest;
import com.printkon.pdp.common.dao.UserDao;
import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.common.enums.AccountStatus;
import com.printkon.pdp.common.enums.ERole;
import com.printkon.pdp.exceptions.DuplicateEmailException;
import com.printkon.pdp.exceptions.DuplicatePhoneException;
import com.printkon.pdp.exceptions.InvalidCredentialsException;
import com.printkon.pdp.exceptions.InvalidRoleException;
import com.printkon.pdp.exceptions.UnauthorizedException;
import com.printkon.pdp.exceptions.UserNotFoundException;
import com.printkon.pdp.notification.EmailService;
import com.printkon.pdp.notification.LinkGeneratorService;
import com.printkon.pdp.notification.config.EmailConfig;
import com.printkon.pdp.role.models.Role;
import com.printkon.pdp.role.repositories.RoleRepository;
import com.printkon.pdp.security.JwtUtils;
import com.printkon.pdp.user.UserDetailsImpl;
import com.printkon.pdp.user.dto.UserRequest;
import com.printkon.pdp.user.dto.UserResponse;
import com.printkon.pdp.user.dto.UserUpdateRequest;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.user.repositories.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserDao userDao;
	private final LinkGeneratorService linkGeneratorService;
	private final EmailConfig emailConfiguration;
	private final EmailService mailService;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtils jwtUtils;
	private final UserRepository userRepository;

	// === CREATE USER (SIGNUP) ===
	public ResponseEntity<ResponseStructure<UserResponse>> createUser(@Valid @RequestBody UserRequest userRequest,
			HttpServletRequest request) {
		ResponseStructure<UserResponse> structure = new ResponseStructure<>();

		// Check duplicates
		if (userDao.existsByEmail(userRequest.getEmail())) {
			throw new DuplicateEmailException("Email already in use: " + userRequest.getEmail());
		}

		if (userDao.existsByPhone(userRequest.getPhone())) {
			throw new DuplicatePhoneException("Phone number already in use: " + userRequest.getPhone());
		}

		// Create user object but do NOT save yet
		User user = mapToUser(userRequest);
		user.setAccountStatus(AccountStatus.IN_ACTIVE);
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		user.setEmailVerified(false);
		user.setLastLoginAt(null);

		// Assign default USER role
		Set<Role> roles = new HashSet<>();
		roleRepository.findByRole(ERole.USER).ifPresentOrElse(roles::add, () -> {
			throw new InvalidRoleException("Default USER role not found in database.");
		});
		user.setRoles(roles);

		// Generate activation link
		String activationLink = linkGeneratorService.getActivationLink(user, request);

		try {
			String emailResponse = mailService.sendRegistrationEmail(user, activationLink);

			if (emailResponse.startsWith("Failed")) {
				throw new RuntimeException("Verification email could not be sent: " + emailResponse);
			}

			// Save only if email succeeded
			user = userDao.saveUser(user);

			structure.setMessage("User created successfully. Verification email sent.");
			structure.setData(mapToUserResponse(user));
			structure.setStatusCode(HttpStatus.CREATED.value());

			return ResponseEntity.status(HttpStatus.CREATED).body(structure);

		} catch (Exception e) {
			// No user saved in DB → avoids stuck state
			structure.setMessage("Signup failed: " + e.getMessage());
			structure.setData(null);
			structure.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(structure);
		}
	}

	// === ACCOUNT ACTIVATION ===
	public ResponseEntity<ResponseStructure<UserResponse>> activate(String token) {
		ResponseStructure<UserResponse> structure = new ResponseStructure<>();

		if (!jwtUtils.validateJwtToken(token)) {
			throw new IllegalArgumentException("Invalid or expired token.");
		}

		String email = jwtUtils.extractUsername(token);
		String type = jwtUtils.extractClaim(token, claims -> claims.get("type", String.class));

		if (!"email-verification".equals(type)) {
			throw new IllegalArgumentException("Token type mismatch.");
		}

		Optional<User> optionalUser = userDao.findByEmail(email);
		if (optionalUser.isEmpty()) {
			throw new UserNotFoundException("User not found.");
		}

		User user = optionalUser.get();
		if (user.getAccountStatus().equals(AccountStatus.ACTIVE)) {
			structure.setMessage("Account is already activated.");
		} else {
			user.setAccountStatus(AccountStatus.ACTIVE);
			user.setEmailVerified(true);
			userDao.saveUser(user);
			structure.setMessage("Account activated successfully.");
		}

		structure.setData(mapToUserResponse(user));
		structure.setStatusCode(HttpStatus.OK.value());

		return ResponseEntity.ok(structure);
	}

	// === READ OPERATIONS ===

	// Get all users
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getAllUsers() {
		List<User> users = userRepository.findAll();

		ResponseStructure<List<UserResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (users.isEmpty()) {
			structure.setMessage("No users found");
			structure.setData(List.of());
		} else {
			List<UserResponse> userResponses = users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
			structure.setMessage("Users fetched successfully");
			structure.setData(userResponses);
		}

		return ResponseEntity.ok(structure);
	}

	// Get user by ID
	public ResponseEntity<ResponseStructure<UserResponse>> getUserById(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		ResponseStructure<UserResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("User fetched successfully");
		structure.setData(mapToUserResponse(user));

		return ResponseEntity.ok(structure);
	}

	// Get user by email
	public ResponseEntity<ResponseStructure<UserResponse>> getUserByEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

		ResponseStructure<UserResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("User fetched successfully");
		structure.setData(mapToUserResponse(user));

		return ResponseEntity.ok(structure);
	}

	// Get current user details
	public ResponseEntity<ResponseStructure<UserResponse>> getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getPrincipal().equals("anonymousUser")) {
			throw new UnauthorizedException("User is not authenticated");
		}

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		User user = userRepository.findByEmail(userDetails.getEmail())
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		ResponseStructure<UserResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("User details fetched successfully");
		structure.setData(mapToUserResponse(user));

		return ResponseEntity.ok(structure);
	}

	// === UPDATE OPERATIONS ===

	// Update user details (requires current password for verification)
	public ResponseEntity<ResponseStructure<UserResponse>> updateUser(Long userId,
			@Valid UserUpdateRequest updateRequest) {

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		// Verify current password
		if (!passwordEncoder.matches(updateRequest.getCurrentPassword(), user.getPassword())) {
			throw new InvalidCredentialsException("Current password is incorrect");
		}

		// Check for duplicate email (if email is being updated)
		if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
			if (userDao.existsByEmail(updateRequest.getEmail())) {
				throw new DuplicateEmailException("Email already in use: " + updateRequest.getEmail());
			}
			user.setEmail(updateRequest.getEmail());
		}

		// Check for duplicate phone (if phone is being updated)
		if (updateRequest.getPhone() != null && !updateRequest.getPhone().equals(user.getPhone())) {
			if (userDao.existsByPhone(updateRequest.getPhone())) {
				throw new DuplicatePhoneException("Phone number already in use: " + updateRequest.getPhone());
			}
			user.setPhone(updateRequest.getPhone());
		}

		// Update other fields if provided
		if (updateRequest.getName() != null) {
			user.setName(updateRequest.getName());
		}
		if (updateRequest.getAge() != null) {
			user.setAge(updateRequest.getAge());
		}
		if (updateRequest.getGender() != null) {
			user.setGender(updateRequest.getGender());
		}

		// Update password if new password is provided
		if (updateRequest.getNewPassword() != null && !updateRequest.getNewPassword().trim().isEmpty()) {
			user.setPassword(passwordEncoder.encode(updateRequest.getNewPassword()));
		}

		user = userDao.saveUser(user);

		ResponseStructure<UserResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("User updated successfully");
		structure.setData(mapToUserResponse(user));

		return ResponseEntity.ok(structure);
	}

	// Update current user (authenticated user updating their own details)
	public ResponseEntity<ResponseStructure<UserResponse>> updateCurrentUser(@Valid UserUpdateRequest updateRequest) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getPrincipal().equals("anonymousUser")) {
			throw new UnauthorizedException("User is not authenticated");
		}

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		User user = userRepository.findByEmail(userDetails.getEmail())
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		return updateUser(user.getId(), updateRequest);
	}

	// === PASSWORD RESET OPERATIONS ===

	// Forgot password - send reset link to email
	public ResponseEntity<ResponseStructure<String>> forgotPassword(@Valid ForgotPasswordRequest forgotPasswordRequest,
			HttpServletRequest request) {

		String email = forgotPasswordRequest.getEmail();
		Optional<User> optionalUser = userRepository.findByEmail(email);

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (optionalUser.isEmpty()) {
			// Don't reveal if email exists or not for security reasons
			structure.setMessage("If the email is registered, a password reset link has been sent");
			structure.setData("Password reset request processed");
			return ResponseEntity.ok(structure);
		}

		User user = optionalUser.get();

		// Generate password reset token
		String resetToken = jwtUtils.generateJwtForPasswordReset(email);
		String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
		String resetLink = baseUrl + "/api/auth/reset-password?token=" + resetToken;

		// Send password reset email
		emailConfiguration.setSubject("Password Reset Request");
		emailConfiguration.setText("Dear " + user.getName() + ",\n\n"
				+ "You have requested to reset your password. Please click on the following link to reset your password:\n\n"
				+ resetLink + "\n\n" + "This link will expire in " + (jwtUtils.getPasswordResetExpiryInSeconds() / 60)
				+ " minutes.\n\n" + "If you did not request this password reset, please ignore this email.\n\n"
				+ "Best regards,\nThe Team");
		emailConfiguration.setToAddress(user.getEmail());

		mailService.sendMail(emailConfiguration);

		structure.setMessage("If the email is registered, a password reset link has been sent");
		structure.setData("Password reset request processed");

		return ResponseEntity.ok(structure);
	}

	// Reset password using token
	public ResponseEntity<ResponseStructure<String>> resetPassword(String token,
			@Valid ResetPasswordRequest resetPasswordRequest) {
		String newPassword = resetPasswordRequest.getNewPassword();

		if (!jwtUtils.validateJwtToken(token)) {
			throw new IllegalArgumentException("Invalid or expired reset token");
		}

		String email = jwtUtils.extractUsername(token);
		String type = jwtUtils.extractClaim(token, claims -> claims.get("type", String.class));

		if (!"password-reset".equals(type)) {
			throw new IllegalArgumentException("Invalid token type");
		}

		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

		// Update password
		user.setPassword(passwordEncoder.encode(newPassword));
		userDao.saveUser(user);

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Password reset successfully");
		structure.setData("Your password has been updated");

		return ResponseEntity.ok(structure);
	}

	// === DELETE OPERATIONS ===

	// Delete user by ID
	public ResponseEntity<ResponseStructure<String>> deleteUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		userRepository.delete(user);

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("User deleted successfully");
		structure.setData("User with ID " + userId + " has been deleted");

		return ResponseEntity.ok(structure);
	}

	// Delete current user (self-deletion with password confirmation)
	public ResponseEntity<ResponseStructure<String>> deleteCurrentUser(String currentPassword) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getPrincipal().equals("anonymousUser")) {
			throw new UnauthorizedException("User is not authenticated");
		}

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		User user = userRepository.findByEmail(userDetails.getEmail())
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		// Verify current password before deletion
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new InvalidCredentialsException("Current password is incorrect");
		}

		userRepository.delete(user);

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Your account has been deleted successfully");
		structure.setData("Account deletion completed");

		return ResponseEntity.ok(structure);
	}

	// === SEARCH AND FILTER OPERATIONS ===

	// Search users by name
	public ResponseEntity<ResponseStructure<List<UserResponse>>> searchUsersByName(String name) {
		List<User> users = userRepository.findByNameContainingIgnoreCase(name);

		ResponseStructure<List<UserResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (users.isEmpty()) {
			structure.setMessage("No users found with name containing: " + name);
			structure.setData(List.of());
		} else {
			List<UserResponse> userResponses = users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
			structure.setMessage("Users found successfully");
			structure.setData(userResponses);
		}

		return ResponseEntity.ok(structure);
	}

	// Get users by account status
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getUsersByAccountStatus(AccountStatus status) {
		List<User> users = userRepository.findByAccountStatus(status);

		ResponseStructure<List<UserResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (users.isEmpty()) {
			structure.setMessage("No users found with status: " + status);
			structure.setData(List.of());
		} else {
			List<UserResponse> userResponses = users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
			structure.setMessage("Users fetched successfully");
			structure.setData(userResponses);
		}

		return ResponseEntity.ok(structure);
	}

	// Get users by role
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getUsersByRole(ERole role) {
		List<User> users = userRepository.findByRole(role);

		ResponseStructure<List<UserResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (users.isEmpty()) {
			structure.setMessage("No users found with role: " + role);
			structure.setData(List.of());
		} else {
			List<UserResponse> userResponses = users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
			structure.setMessage("Users with role " + role + " fetched successfully");
			structure.setData(userResponses);
		}

		return ResponseEntity.ok(structure);
	}

	// Get users by role and status
	public ResponseEntity<ResponseStructure<List<UserResponse>>> getUsersByRoleAndStatus(ERole role,
			AccountStatus status) {
		List<User> users = userRepository.findByRoleAndAccountStatus(role, status);

		ResponseStructure<List<UserResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (users.isEmpty()) {
			structure.setMessage("No users found with role: " + role + " and status: " + status);
			structure.setData(List.of());
		} else {
			List<UserResponse> userResponses = users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
			structure.setMessage("Users fetched successfully");
			structure.setData(userResponses);
		}

		return ResponseEntity.ok(structure);
	}

	// Check if user has specific role
	public boolean userHasRole(String email, ERole role) {
		return userRepository.existsByEmailAndRole(email, role);
	}

	// Get count by role
	public ResponseEntity<ResponseStructure<Long>> getUserCountByRole(ERole role) {
		long count = userRepository.countByRole(role);

		ResponseStructure<Long> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("User count for role " + role + " retrieved successfully");
		structure.setData(count);

		return ResponseEntity.ok(structure);
	}

	// === UTILITY METHODS ===

	// Check if user exists by email
	public boolean existsByEmail(String email) {
		return userDao.existsByEmail(email);
	}

	// Check if user exists by phone
	public boolean existsByPhone(String phone) {
		return userDao.existsByPhone(phone);
	}

	// Get total user count
	public ResponseEntity<ResponseStructure<Long>> getUserCount() {
		long count = userRepository.count();

		ResponseStructure<Long> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Total user count retrieved successfully");
		structure.setData(count);

		return ResponseEntity.ok(structure);
	}

	// === HELPER METHODS ===

	private User mapToUser(UserRequest userRequest) {
		return User.builder().email(userRequest.getEmail()).name(userRequest.getName()).phone(userRequest.getPhone())
				.gender(userRequest.getGender()).age(userRequest.getAge()).password(userRequest.getPassword())
				.accountStatus(AccountStatus.IN_ACTIVE).isEmailVerified(false).lastLoginAt(null).build();
	}

	private UserResponse mapToUserResponse(User user) {
		Set<String> roleNames = user.getRoles().stream().map(role -> role.getRole().name()).collect(Collectors.toSet());

		return UserResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail())
				.phone(user.getPhone()).gender(user.getGender()).age(user.getAge()).roles(roleNames)
				.accountStatus(user.getAccountStatus()).build();
	}
}