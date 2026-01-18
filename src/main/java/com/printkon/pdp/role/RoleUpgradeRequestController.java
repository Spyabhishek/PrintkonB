package com.printkon.pdp.role;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.role.dto.RoleUpgradeResponse;
import com.printkon.pdp.role.models.RoleUpgradeRequest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/role-upgrade")
@RequiredArgsConstructor
public class RoleUpgradeRequestController {

	private final RoleUpgradeRequestService requestService;

	// User submits upgrade request
	@PostMapping("/request")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<ResponseStructure<RoleUpgradeResponse>> requestUpgrade(
			@Validated @RequestBody RoleUpgradeRequest roleUpgradeRequest, Principal principal,
			HttpServletRequest request) {

		return requestService.createUpgradeRequestByEmail(principal.getName(), roleUpgradeRequest, request);
	}
	
	//verify user before request role-upgrade
	@GetMapping("/verify")
    public ResponseEntity<ResponseStructure<String>> verifyUpgradeRequest(@RequestParam("token") String token) {
        return requestService.verifyUserUpgradeRequest(token);
    }

	// SUPER_ADMIN views all pending requests
	@GetMapping("/pending")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<java.util.List<RoleUpgradeResponse>>> getPendingRequests() {
		return requestService.getPendingRequests();
	}

	// SUPER_ADMIN approves request by ID
	@PostMapping("/approve/{requestId}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<RoleUpgradeResponse>> approveRequest(@PathVariable Long requestId) {
		return requestService.approveRequest(requestId);
	}

	// SUPER_ADMIN rejects request
	@PostMapping("/reject/{requestId}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<RoleUpgradeResponse>> rejectRequest(@PathVariable Long requestId) {
		return requestService.rejectRequest(requestId);
	}

	// SUPER_ADMIN downgrade admin
	@PutMapping("/downgrade/{userId}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ResponseEntity<ResponseStructure<String>> downgradeUser(@PathVariable Long userId,
			@RequestParam String reason) {
		return requestService.downgradeToUser(userId, reason);
	}

}
