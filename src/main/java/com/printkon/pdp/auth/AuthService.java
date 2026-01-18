package com.printkon.pdp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

import com.printkon.pdp.auth.dto.LoginRequest;
import com.printkon.pdp.auth.models.JwtBlacklist;
import com.printkon.pdp.auth.models.LoginAttempt;
import com.printkon.pdp.auth.models.RefreshToken;
import com.printkon.pdp.auth.repositories.JwtBlacklistRepository;
import com.printkon.pdp.auth.repositories.LoginAttemptRepository;
import com.printkon.pdp.auth.repositories.RefreshTokenRepository;
import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.exceptions.TokenCompromiseException;
import com.printkon.pdp.security.JwtUtils;
import com.printkon.pdp.user.UserDetailsImpl;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.user.repositories.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final RefreshTokenRepository refreshTokenRepo;
	private final UserRepository userRepository;
	private final LoginAttemptRepository loginAttemptRepository;
	private final JwtBlacklistRepository jwtBlacklistRepository;

	// --- Constants ---
	private static final SecureRandom secureRandom = new SecureRandom();

	// --- LOGIN ---
	@Transactional
	public ResponseEntity<ResponseStructure<Map<String, Object>>> authenticateUser(LoginRequest loginRequest,
			HttpServletRequest httpRequest) {

		String loginIdentifier = loginRequest.getEmail().toLowerCase();
		String remoteAddr = httpRequest.getRemoteAddr();

		checkBruteForce(loginIdentifier, remoteAddr);

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
		} catch (BadCredentialsException e) {
			handleFailedLogin(loginIdentifier, remoteAddr);
			throw e;
		}

		resetBruteForceCounter(loginIdentifier, remoteAddr);
		log.info("User '{}' authenticated successfully.", loginIdentifier);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		User user = userRepository.findByEmail(userDetails.getEmail()).orElseThrow(() -> new UsernameNotFoundException(
				"User not found in repository despite successful authentication for email: " + userDetails.getEmail()));

		// Update last login time
		user.setLastLoginAt(Instant.now());
		userRepository.save(user);

		// Raw values
		String rawUserAgent = loginRequest.getUserAgent() != null ? loginRequest.getUserAgent()
				: httpRequest.getHeader("User-Agent");
		String deviceId = loginRequest.getDeviceId();
		String ipAddress = remoteAddr;

		// Hashed device signature for security
		String deviceInfo = hashDeviceInfo(rawUserAgent, ipAddress, user.getId());

		String accessToken = jwtUtils.generateAccessToken(authentication);
		String clearTextRefreshToken = generateSecureToken();

		// Pass raw and hashed info
		createAndSaveRefreshToken(user, deviceInfo, clearTextRefreshToken, deviceId, rawUserAgent, ipAddress);

		ResponseCookie accessCookie = createCookie("jwt", accessToken, jwtUtils.getAccessTokenTtl(), "/", "None");
		ResponseCookie refreshCookie = createCookie("refresh", clearTextRefreshToken, jwtUtils.getRefreshTokenTtl(),
				"/api/auth/refresh", "None");

		// IMPROVED: Create comprehensive user data response
		Map<String, Object> userData = createUserDataResponse(user, userDetails);

		ResponseStructure<Map<String, Object>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Login successful");
		structure.setData(Map.of("user", userData, "tokenInfo",
				Map.of("accessExpiresIn", jwtUtils.getAccessTokenExpiryInSeconds(), "refreshExpiresIn",
						jwtUtils.getRefreshTokenExpiryInSeconds())));

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accessCookie.toString())
				.header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(structure);
	}

	// --- REFRESH ---
	@Transactional
	public ResponseEntity<ResponseStructure<Map<String, Object>>> refreshToken(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, "refresh");
		if (cookie == null) {
			throw new IllegalArgumentException("Refresh token cookie is missing.");
		}

		String oldClearTextToken = cookie.getValue();
		String oldHashedToken = hashToken(oldClearTextToken);

		RefreshToken oldToken = refreshTokenRepo.findById(oldHashedToken)
				.orElseThrow(() -> new TokenCompromiseException("Refresh token not found. Session terminated."));

		if (oldToken.isRevoked()) {
			log.warn("CRITICAL: Revoked refresh token used for user ID '{}'. Revoking all sessions.",
					oldToken.getUser().getId());
			revokeTokenFamily(oldToken.getUser());
			throw new TokenCompromiseException(
					"Token has been revoked. All sessions terminated for user: " + oldToken.getUser().getId());
		}

		if (oldToken.getExpiresAt().isBefore(Instant.now())) {
			revokeTokenFamily(oldToken.getUser());
			throw new IllegalStateException(
					"Refresh token has expired. All sessions terminated for user: " + oldToken.getUser().getId());
		}

		// Atomically rotate the token
		String newClearTextToken = generateSecureToken();
		String newHashedToken = hashToken(newClearTextToken);

		// The findAndRevokeById ensures the old token is not already revoked,
		// preventing reuse.
		int rowsAffected = refreshTokenRepo.findAndRevokeById(oldHashedToken, newHashedToken);
		if (rowsAffected == 0) {
			log.warn(
					"CRITICAL: Refresh token reuse or race condition detected for user ID '{}'. Revoking all sessions.",
					oldToken.getUser().getId());
			revokeTokenFamily(oldToken.getUser());
			throw new TokenCompromiseException(
					"Token reuse detected. All sessions terminated for user: " + oldToken.getUser().getId());
		}

		// Create the new token only after successfully revoking the old one
		createAndSaveRefreshToken(oldToken.getUser(), oldToken.getDeviceInfo(), // hashed
				newClearTextToken, oldToken.getDeviceId(), // raw UUID
				oldToken.getUserAgent(), // raw UA
				oldToken.getIpAddress() // raw IP
		);

		log.info("Refreshed token for user '{}'.", oldToken.getUser().getEmail());

		// IMPROVED: Get fresh user data from database
		User user = userRepository.findById(oldToken.getUser().getId())
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + oldToken.getUser().getId()));

		Authentication authentication = createAuthenticationFromUser(user);
		String newAccessToken = jwtUtils.generateAccessToken(authentication);

		ResponseCookie accessCookie = createCookie("jwt", newAccessToken, jwtUtils.getAccessTokenTtl(), "/", "None");
		ResponseCookie refreshCookieResp = createCookie("refresh", newClearTextToken, jwtUtils.getRefreshTokenTtl(),
				"/api/auth/refresh", "None");

		// IMPROVED: Include fresh user data in refresh response
		UserDetailsImpl userDetails = UserDetailsImpl.build(user);
		Map<String, Object> userData = createUserDataResponse(user, userDetails);

		ResponseStructure<Map<String, Object>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Token refreshed successfully");
		structure.setData(Map.of("user", userData, "tokenInfo",
				Map.of("accessExpiresIn", jwtUtils.getAccessTokenExpiryInSeconds(), "refreshExpiresIn",
						jwtUtils.getRefreshTokenExpiryInSeconds())));

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accessCookie.toString())
				.header(HttpHeaders.SET_COOKIE, refreshCookieResp.toString()).body(structure);
	}

	// --- LOGOUT ---
	@Transactional
	public ResponseEntity<ResponseStructure<String>> logout(HttpServletRequest request) {
		blacklistAccessToken(WebUtils.getCookie(request, "jwt"));

		Cookie refreshCookie = WebUtils.getCookie(request, "refresh");
		if (refreshCookie != null) {
			String hashedToken = hashToken(refreshCookie.getValue());
			refreshTokenRepo.findById(hashedToken).ifPresent(rt -> {
				rt.setRevoked(true);
				refreshTokenRepo.save(rt);
				log.info("User '{}' logged out. Refresh token revoked.", rt.getUser().getEmail());
			});
		}

		ResponseCookie clearAccess = createCookie("jwt", "", Duration.ZERO, "/", "None");
		ResponseCookie clearRefresh = createCookie("refresh", "", Duration.ZERO, "/api/auth/refresh", "None");

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Logged out successfully");

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clearAccess.toString())
				.header(HttpHeaders.SET_COOKIE, clearRefresh.toString()).body(structure);
	}

	// --- HELPER METHOD ---
	/**
	 * Creates a consistent user data response structure
	 */
	private Map<String, Object> createUserDataResponse(User user, UserDetailsImpl userDetails) {
		// Extract role names from authorities
		List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.map(role -> role.startsWith("ROLE_") ? role.substring(5) : role).collect(Collectors.toList());

		return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(), "roles", roles,
				"accountStatus", user.getAccountStatus(), "isEmailVerified", user.isEmailVerified(), "lastLoginAt",
				user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "", "createdAt",
				user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
	}

	// --- Security & Helper Methods ---

	private void createAndSaveRefreshToken(User user, String deviceInfo, String clearTextToken, String deviceId,
			String userAgent, String ipAddress) {
		RefreshToken refreshToken = RefreshToken.builder().id(hashToken(clearTextToken)).user(user)
				.issuedAt(Instant.now()).expiresAt(Instant.now().plus(jwtUtils.getRefreshTokenTtl())).revoked(false)
				.deviceInfo(deviceInfo) // hashed signature
				.deviceId(deviceId) // raw UUID from frontend
				.userAgent(userAgent) // raw browser UA
				.ipAddress(ipAddress) // raw IP
				.build();

		refreshTokenRepo.save(refreshToken);
	}

	private void revokeTokenFamily(User user) {
		List<RefreshToken> activeTokens = refreshTokenRepo.findByUserAndRevokedIsFalse(user);
		if (!activeTokens.isEmpty()) {
			activeTokens.forEach(t -> t.setRevoked(true));
			refreshTokenRepo.saveAll(activeTokens);
			log.info("Revoked {} active refresh tokens for user '{}'", activeTokens.size(), user.getEmail());
		}
	}

	private String hashDeviceInfo(String userAgent, String remoteAddr, Long userId) {
		String input = (userAgent != null ? userAgent : "") + "|" + (remoteAddr != null ? remoteAddr : "") + "|"
				+ userId.toString() + "|" + jwtUtils.getDeviceHashPepper();
		return sha256(input);
	}

	private String hashToken(String token) {
		return sha256(token);
	}

	private String sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available.", e);
		}
	}

	private String generateSecureToken() {
		byte[] randomBytes = new byte[32]; // 32 bytes = 256 bits
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private ResponseCookie createCookie(String name, String value, Duration maxAge, String path, String sameSite) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value).httpOnly(true)
				.secure(jwtUtils.isCookieSecure()).path(path).maxAge(maxAge);

		String cookieDomain = jwtUtils.getCookieDomain();
		if (cookieDomain != null && !cookieDomain.isBlank())
			builder.domain(cookieDomain);

		builder.sameSite("None");
		return builder.build();
	}

	private Authentication createAuthenticationFromUser(User user) {
		UserDetailsImpl userDetails = UserDetailsImpl.build(user);
		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	}

	// --- Brute-Force and Blacklist Logic (using MySQL) ---

	private void checkBruteForce(String identifier, String remoteAddr) {
		loginAttemptRepository.findByIdentifierAndRemoteAddr(identifier, remoteAddr).ifPresent(attempt -> {
			if (attempt.getAttempts() >= jwtUtils.getMaxLoginAttempts()
					&& attempt.getLockedUntil().isAfter(Instant.now())) {
				log.warn("Login blocked for user '{}' from IP '{}' due to too many failed attempts.", identifier,
						remoteAddr);
				throw new BadCredentialsException("Account is locked due to too many failed login attempts.");
			}
		});
	}

	private void handleFailedLogin(String identifier, String remoteAddr) {
		LoginAttempt attempt = loginAttemptRepository.findByIdentifierAndRemoteAddr(identifier, remoteAddr)
				.orElseGet(() -> new LoginAttempt(identifier, remoteAddr));

		attempt.setAttempts(attempt.getAttempts() + 1);

		if (attempt.getAttempts() >= jwtUtils.getMaxLoginAttempts()) {
			attempt.setLockedUntil(Instant.now().plus(jwtUtils.getLoginLockoutDuration()));
			log.warn("Login for user '{}' from IP '{}' is now locked for {} minutes.", identifier, remoteAddr,
					jwtUtils.getLoginLockoutDuration().toMinutes());
		}
		loginAttemptRepository.save(attempt);
		log.warn("Failed login attempt #{} for user '{}' from IP '{}'.", attempt.getAttempts(), identifier, remoteAddr);
	}

	private void resetBruteForceCounter(String identifier, String remoteAddr) {
		loginAttemptRepository.findByIdentifierAndRemoteAddr(identifier, remoteAddr)
				.ifPresent(loginAttemptRepository::delete);
	}

	private void blacklistAccessToken(Cookie jwtCookie) {
		if (jwtCookie == null || jwtCookie.getValue().isEmpty()) {
			return;
		}
		try {
			String token = jwtCookie.getValue();
			String jti = jwtUtils.extractJti(token);
			Instant expirationTime = jwtUtils.getExpirationTime(token);
			if (expirationTime.isAfter(Instant.now())) {
				JwtBlacklist blacklistEntry = new JwtBlacklist(jti, expirationTime);
				jwtBlacklistRepository.save(blacklistEntry);
			}
		} catch (Exception e) {
			log.error("Could not blacklist token. It might be invalid/expired.", e);
		}
	}
}