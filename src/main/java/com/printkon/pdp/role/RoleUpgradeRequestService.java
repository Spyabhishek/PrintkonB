package com.printkon.pdp.role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.common.enums.ERole;
import com.printkon.pdp.common.enums.RequestStatus;
import com.printkon.pdp.exceptions.UserNotFoundException;
import com.printkon.pdp.notification.EmailService;
import com.printkon.pdp.notification.LinkGeneratorService;
import com.printkon.pdp.notification.config.EmailConfig;
import com.printkon.pdp.role.dto.RoleUpgradeResponse;
import com.printkon.pdp.role.models.Role;
import com.printkon.pdp.role.models.RoleUpgradeRequest;
import com.printkon.pdp.role.repositories.RoleRepository;
import com.printkon.pdp.role.repositories.RoleUpgradeRequestRepository;
import com.printkon.pdp.security.JwtUtils;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.user.repositories.UserRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleUpgradeRequestService {

	private final RoleUpgradeRequestRepository requestRepo;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final JwtUtils jwtUtils;
	private final EmailService emailService;
	private final LinkGeneratorService linkGeneratorService;

	public ResponseEntity<ResponseStructure<RoleUpgradeResponse>> createUpgradeRequestByEmail(String email,
			RoleUpgradeRequest dto, HttpServletRequest httpRequest) {

		ResponseStructure<RoleUpgradeResponse> structure = new ResponseStructure<>();

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

		if (!requestRepo.findByUser(user).isEmpty()) {
			throw new IllegalStateException("Upgrade request already submitted");
		}

		RoleUpgradeRequest request = new RoleUpgradeRequest();
		request.setUser(user);
		request.setCompanyName(dto.getCompanyName());
		request.setGstNumber(dto.getGstNumber());
		request.setCustomNote(dto.getCustomNote()); // NEW LINE
		request.setRequestDate(LocalDateTime.now());
		request.setStatus(RequestStatus.UNCONFIRMED);

		// Save to DB
		request = requestRepo.save(request);

		// Generate confirmation link
		String confirmationLink = linkGeneratorService.getUpgradeConfirmationLink(user, request.getId(), httpRequest);

		// Send confirmation email
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Important: Confirm Your Role Upgrade Request");
		emailConfig.setText("""
				Dear %s,

				We received your request to upgrade your role to ADMIN on PrintLok.

				Please confirm this request by clicking the link below:
				%s

				This link will expire in 15 minutes.

				Regards,
				PrintLok Team
				""".formatted(user.getName() != null ? user.getName() : "User", confirmationLink));

		emailService.sendMail(emailConfig);

		// Prepare response
		RoleUpgradeResponse response = mapToResponse(request);
		structure.setMessage("Upgrade request submitted. Please confirm via the email link.");
		structure.setData(response);
		structure.setStatusCode(HttpStatus.CREATED.value());

		return ResponseEntity.status(HttpStatus.CREATED).body(structure);
	}

	public ResponseEntity<ResponseStructure<String>> verifyUserUpgradeRequest(String token) {
		Claims claims = jwtUtils.extractAllClaims(token);

		if (!"role-upgrade".equals(claims.get("type"))) {
			throw new IllegalArgumentException("Invalid token type");
		}

		Long requestId = Long.valueOf(claims.get("requestId").toString());

		RoleUpgradeRequest request = requestRepo.findById(requestId)
				.orElseThrow(() -> new NoSuchElementException("Request not found"));

		if (!request.getStatus().equals(RequestStatus.UNCONFIRMED)) {
			throw new IllegalStateException("Request is already verified or processed.");
		}

		// Update status to PENDING
		request.setStatus(RequestStatus.PENDING);
		requestRepo.save(request);

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setMessage("Role upgrade request verified successfully.");
		structure.setData("Your request has been verified and is now pending approval.");
		structure.setStatusCode(HttpStatus.OK.value());

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<List<RoleUpgradeResponse>>> getPendingRequests() {
		List<RoleUpgradeRequest> pending = requestRepo.findByStatus(RequestStatus.PENDING);

		ResponseStructure<List<RoleUpgradeResponse>> structure = new ResponseStructure<>();

		if (pending.isEmpty()) {
			structure.setMessage("No pending upgrade requests found");
			structure.setData(List.of());
			structure.setStatusCode(HttpStatus.OK.value());
			return ResponseEntity.ok(structure);
		}

		List<RoleUpgradeResponse> responseList = pending.stream().map(this::mapToResponse).toList();

		structure.setMessage("Pending upgrade requests fetched successfully");
		structure.setData(responseList);
		structure.setStatusCode(HttpStatus.OK.value());

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<RoleUpgradeResponse>> approveRequest(Long requestId) {
		RoleUpgradeRequest request = requestRepo.findById(requestId)
				.orElseThrow(() -> new NoSuchElementException("Request not found"));

		request.setStatus(RequestStatus.APPROVED);
		requestRepo.save(request);

		User user = request.getUser();
		Role adminRole = roleRepository.findByRole(ERole.ADMIN)
				.orElseThrow(() -> new RuntimeException("Admin role not found"));

		user.getRoles().add(adminRole);
		userRepository.save(user);

		// Send email notification
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Important: Your Role Upgrade Request is Approved!");
		emailConfig.setText("""
					Dear %s,

					We're happy to inform you that your request to become an ADMIN has been approved!
					You now have access to advanced administrative features within the PrintLok system.
					Thank you for trusting our platform.

					Regards,
					PrintLok Team
				""".formatted(user.getName() != null ? user.getName() : "User"));

		emailService.sendMail(emailConfig);

		RoleUpgradeResponse response = mapToResponse(request);

		ResponseStructure<RoleUpgradeResponse> structure = new ResponseStructure<>();
		structure.setMessage("Request approved and user upgraded to ADMIN");
		structure.setData(response);
		structure.setStatusCode(HttpStatus.OK.value());

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<RoleUpgradeResponse>> rejectRequest(Long requestId) {
		RoleUpgradeRequest request = requestRepo.findById(requestId)
				.orElseThrow(() -> new NoSuchElementException("Request not found"));

		request.setStatus(RequestStatus.REJECTED);
		request = requestRepo.save(request);

		RoleUpgradeResponse response = mapToResponse(request);

		ResponseStructure<RoleUpgradeResponse> structure = new ResponseStructure<>();
		structure.setMessage("Request has been rejected");
		structure.setData(response);
		structure.setStatusCode(HttpStatus.OK.value());

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<String>> downgradeToUser(Long userId, String reason) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		// Remove ADMIN role if present
		boolean wasAdmin = user.getRoles().removeIf(role -> role.getRole() == ERole.ADMIN);

		if (!wasAdmin) {
			throw new IllegalStateException("User is not an ADMIN.");
		}

		userRepository.save(user);

		// Send downgrade email
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Important: Account Downgraded");
		emailConfig.setText(
				"""
							Dear %s,

							We would like to inform you that your ADMIN access has been revoked and your account has been downgraded to a regular USER.

							Reason: %s

							If you believe this was a mistake or need further clarification, please contact our support team.

							Regards,
							PrintLok Support
						"""
						.formatted(user.getName() != null ? user.getName() : "User", reason));

		emailService.sendMail(emailConfig);

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setMessage("User downgraded successfully");
		structure.setData("User with ID " + userId + " has been downgraded to USER.");
		structure.setStatusCode(HttpStatus.OK.value());

		return ResponseEntity.ok(structure);
	}

	// Mapping method
	private RoleUpgradeResponse mapToResponse(RoleUpgradeRequest request) {
		RoleUpgradeResponse response = new RoleUpgradeResponse();
		response.setRequestId(request.getId());
		response.setCompanyName(request.getCompanyName());
		response.setGstNumber(request.getGstNumber());
		response.setUserEmail(request.getUser().getEmail());
		response.setCustomNote(request.getCustomNote()); // NEW LINE
		response.setStatus(request.getStatus());
		response.setRequestDate(request.getRequestDate());
		return response;
	}

}
