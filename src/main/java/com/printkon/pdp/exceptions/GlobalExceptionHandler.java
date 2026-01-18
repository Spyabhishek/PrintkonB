package com.printkon.pdp.exceptions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.printkon.pdp.common.dto.ResponseStructure;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	// === AUTHENTICATION & AUTHORIZATION EXCEPTIONS ===

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ResponseStructure<String>> handleBadCredentials(BadCredentialsException ex) {
		log.warn("Authentication Failed: Invalid email or password provided - {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.UNAUTHORIZED.value()).message("Authentication Failed")
				.data("Invalid email or password provided").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(TokenCompromiseException.class)
	public ResponseEntity<ResponseStructure<String>> handleTokenCompromise(TokenCompromiseException ex) {
		log.warn("Token Compromise Detected: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.UNAUTHORIZED.value()).message("Session Invalid")
				.data("Your session has been terminated for security reasons. Please log in again")
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ResponseStructure<String>> handleUnauthorized(UnauthorizedException ex) {
		log.warn("Unauthorized access attempt: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.UNAUTHORIZED.value()).message("Access denied").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ResponseStructure<String>> handleAccessDenied(AccessDeniedException ex) {
		log.warn("Access denied: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.FORBIDDEN.value()).message("Access forbidden")
				.data("You don't have permission to access this resource").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ResponseStructure<String>> handleInvalidCredentials(InvalidCredentialsException ex) {
		log.warn("Invalid credentials: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.UNAUTHORIZED.value()).message("Authentication failed").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	// === USER DATA EXCEPTIONS ===

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ResponseStructure<String>> handleUserNotFound(UserNotFoundException ex) {
		log.warn("User not found: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.NOT_FOUND.value()).message("User not found").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler({ DuplicateEmailException.class, DuplicatePhoneException.class })
	public ResponseEntity<ResponseStructure<String>> handleDuplicateData(RuntimeException ex) {
		log.warn("Duplicate data conflict: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.CONFLICT.value()).message("Data conflict").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(InvalidRoleException.class)
	public ResponseEntity<ResponseStructure<String>> handleInvalidRole(InvalidRoleException ex) {
		log.error("Invalid role assignment: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message("Role assignment failed").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	// === VALIDATION EXCEPTIONS ===

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ResponseStructure<Map<String, String>>> handleValidationErrors(
			MethodArgumentNotValidException ex) {
		log.warn("Validation errors occurred: {}", ex.getMessage());

		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

		ex.getBindingResult().getGlobalErrors()
				.forEach(error -> errors.put(error.getObjectName(), error.getDefaultMessage()));

		ResponseStructure<Map<String, String>> response = ResponseStructure.<Map<String, String>>builder()
				.success(false).statusCode(HttpStatus.BAD_REQUEST.value()).message("Validation failed").data(errors)
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ResponseStructure<String>> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Invalid argument: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message("Invalid request").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ResponseStructure<String>> handleIllegalState(IllegalStateException ex) {
		log.debug("Illegal state encountered in auth flow: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message("Invalid Request").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	// === JWT & TOKEN EXCEPTIONS ===

	@ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
	public ResponseEntity<ResponseStructure<String>> handleExpiredJwt(io.jsonwebtoken.ExpiredJwtException ex) {
		log.warn("JWT token expired: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.UNAUTHORIZED.value()).message("Token expired")
				.data("Your session has expired. Please login again").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(io.jsonwebtoken.MalformedJwtException.class)
	public ResponseEntity<ResponseStructure<String>> handleMalformedJwt(io.jsonwebtoken.MalformedJwtException ex) {
		log.warn("Malformed JWT token: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message("Invalid token").data("Token format is invalid")
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(io.jsonwebtoken.security.SignatureException.class)
	public ResponseEntity<ResponseStructure<String>> handleJwtSignature(
			io.jsonwebtoken.security.SignatureException ex) {
		log.warn("JWT signature validation failed: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.UNAUTHORIZED.value()).message("Token validation failed")
				.data("Invalid token signature").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ResponseStructure<String>> handleAuthenticationException(AuthenticationException ex) {
		log.warn("Authentication failed: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.UNAUTHORIZED.value()).message("Authentication failed")
				.data("Please check your credentials and try again").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	// === EMAIL & COMMUNICATION EXCEPTIONS ===

	@ExceptionHandler(org.springframework.mail.MailException.class)
	public ResponseEntity<ResponseStructure<String>> handleMailException(org.springframework.mail.MailException ex) {
		log.error("Email sending failed: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.SERVICE_UNAVAILABLE.value()).message("Email service error")
				.data("Failed to send email. Please try again later").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	}

	// === DATABASE EXCEPTIONS ===

	@ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
	public ResponseEntity<ResponseStructure<String>> handleDataIntegrityViolation(
			org.springframework.dao.DataIntegrityViolationException ex) {
		log.error("Data integrity violation: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.CONFLICT.value()).message("Data constraint violation")
				.data("The operation violates data integrity constraints").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(org.springframework.dao.DataAccessException.class)
	public ResponseEntity<ResponseStructure<String>> handleDataAccessException(
			org.springframework.dao.DataAccessException ex) {
		log.error("Database access error: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).message("Database error")
				.data("A database error occurred. Please try again later").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	// === HTTP & REQUEST EXCEPTIONS ===

	@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ResponseStructure<String>> handleMethodNotSupported(
			org.springframework.web.HttpRequestMethodNotSupportedException ex) {
		log.warn("Method not supported: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.METHOD_NOT_ALLOWED.value()).message("Method not allowed")
				.data("HTTP method " + ex.getMethod() + " is not supported for this endpoint")
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
	}

	@ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
	public ResponseEntity<ResponseStructure<String>> handleMissingParameter(
			org.springframework.web.bind.MissingServletRequestParameterException ex) {
		log.warn("Missing request parameter: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message("Missing required parameter")
				.data("Required parameter '" + ex.getParameterName() + "' is missing").timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ResponseStructure<String>> handleTypeMismatch(
			org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
		log.warn("Parameter type mismatch: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message("Invalid parameter type")
				.data("Parameter '" + ex.getName() + "' should be of type " + ex.getRequiredType().getSimpleName())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	// === CUSTOM APPLICATION EXCEPTIONS ===

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ResponseStructure<?>> handleResourceNotFound(ResourceNotFoundException ex) {
		log.warn("Resource not found: {}", ex.getMessage());

		ResponseStructure<?> response = ResponseStructure.builder().success(false)
				.statusCode(HttpStatus.NOT_FOUND.value()).message(ex.getMessage()).data(null)
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(InvalidOperationException.class)
	public ResponseEntity<ResponseStructure<?>> handleInvalidOperation(InvalidOperationException ex) {
		log.warn("Invalid operation: {}", ex.getMessage());

		ResponseStructure<?> response = ResponseStructure.builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message(ex.getMessage()).data(null)
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(BusinessValidationException.class)
	public ResponseEntity<ResponseStructure<?>> handleBusinessValidation(BusinessValidationException ex) {
		log.warn("Business validation failed: {}", ex.getMessage());

		ResponseStructure<?> response = ResponseStructure.builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message(ex.getMessage()).data(null)
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ResponseStructure<Object>> handleBusinessRule(BusinessRuleException ex) {
		log.warn("Business rule violation: {}", ex.getMessage());

		ResponseStructure<Object> response = ResponseStructure.builder().success(false)
				.statusCode(HttpStatus.BAD_REQUEST.value()).message(ex.getMessage()).data(null)
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	// === ADDRESS RELATED EXCEPTIONS ===

	@ExceptionHandler(AddressNotFoundException.class)
	public ResponseEntity<ResponseStructure<String>> handleAddressNotFound(AddressNotFoundException ex) {
		log.warn("Address not found: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.NOT_FOUND.value()).message("Address not found").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	// === VALIDATION EXCEPTIONS (Additional) ===

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ResponseStructure<Map<String, String>>> handleConstraintViolationException(
			ConstraintViolationException ex) {
		log.warn("Constraint violation: {}", ex.getMessage());

		Map<String, String> errors = ex.getConstraintViolations().stream().collect(
				Collectors.toMap(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage));

		ResponseStructure<Map<String, String>> response = ResponseStructure.<Map<String, String>>builder()
				.success(false).statusCode(HttpStatus.BAD_REQUEST.value()).message("Validation failed").data(errors)
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ResponseStructure<String>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
		log.warn("File size exceeded: {}", ex.getMessage());

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value()).message("File size exceeds maximum allowed size")
				.data("Maximum file size is 5MB").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
	}

	// === GENERIC EXCEPTION HANDLER (FALLBACK) ===

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ResponseStructure<String>> handleGlobalException(Exception ex, WebRequest request) {
		log.error("Unexpected error occurred at {}: {}", request.getDescription(false), ex.getMessage(), ex);

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).message("Internal server error")
				.data("An unexpected error occurred. Please try again later").timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ResponseStructure<String>> handleRuntimeException(RuntimeException ex) {
		log.error("Runtime exception: {}", ex.getMessage(), ex);

		ResponseStructure<String> response = ResponseStructure.<String>builder().success(false)
				.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).message("Operation failed").data(ex.getMessage())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}