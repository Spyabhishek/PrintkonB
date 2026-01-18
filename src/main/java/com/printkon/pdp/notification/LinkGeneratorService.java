package com.printkon.pdp.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.printkon.pdp.security.JwtUtils;
import com.printkon.pdp.user.models.User;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LinkGeneratorService {

	@Autowired
	private JwtUtils jwtUtils;

	/**
	 * Generates activation link for email verification
	 */
	public String getActivationLink(User user, HttpServletRequest request) {
		String token = jwtUtils.generateJwtForEmail(user.getEmail());
		String baseUrl = getBaseUrl(request);
		return baseUrl + "/api/auth/activate?token=" + token;
	}

	/**
	 * Generates role upgrade confirmation link
	 */
	public String getUpgradeConfirmationLink(User user, Long requestId, HttpServletRequest request) {
		String token = jwtUtils.generateRoleUpgradeToken(user.getEmail(), requestId);
		String baseUrl = getBaseUrl(request);
		return baseUrl + "/api/role-upgrade/verify?token=" + token;
	}

	/**
	 * Generates password reset link
	 */
	public String getPasswordResetLink(String email, HttpServletRequest request) {
		String token = jwtUtils.generateJwtForPasswordReset(email);
		String baseUrl = getBaseUrl(request);
		return baseUrl + "/api/users/reset-password?token=" + token;
	}

	/**
	 * Generates password reset link with frontend URL (for SPA applications)
	 */
	public String getPasswordResetLinkForFrontend(String email, HttpServletRequest request, String frontendBaseUrl) {
		String token = jwtUtils.generateJwtForPasswordReset(email);
		return frontendBaseUrl + "/reset-password?token=" + token;
	}

	/**
	 * Generates email verification link for frontend
	 */
	public String getActivationLinkForFrontend(User user, HttpServletRequest request, String frontendBaseUrl) {
		String token = jwtUtils.generateJwtForEmail(user.getEmail());
		return frontendBaseUrl + "/verify-email?token=" + token;
	}

	/**
	 * Extracts base URL from request
	 */
	private String getBaseUrl(HttpServletRequest request) {
		return request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
	}

	/**
	 * Generates secure link with custom expiry
	 */
	public String generateSecureLink(String email, String linkType, HttpServletRequest request, String endpoint) {
		String token;

		switch (linkType.toLowerCase()) {
		case "email-verification":
			token = jwtUtils.generateJwtForEmail(email);
			break;
		case "password-reset":
			token = jwtUtils.generateJwtForPasswordReset(email);
			break;
		default:
			throw new IllegalArgumentException("Unsupported link type: " + linkType);
		}

		String baseUrl = getBaseUrl(request);
		return baseUrl + endpoint + "?token=" + token;
	}

	/**
	 * Validates if a link token is still valid
	 */
	public boolean isLinkValid(String token) {
		return jwtUtils.validateJwtToken(token);
	}

	/**
	 * Gets remaining time for a link token in minutes
	 */
	public long getRemainingTimeInMinutes(String token) {
		return jwtUtils.getRemainingTime(token) / (1000 * 60);
	}
}