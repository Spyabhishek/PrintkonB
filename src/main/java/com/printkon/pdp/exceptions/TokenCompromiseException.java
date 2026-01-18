package com.printkon.pdp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a compromised or reused refresh token is detected. Results in a
 * 401 Unauthorized response via the ControllerAdvice.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenCompromiseException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TokenCompromiseException(String message) {
		super(message);
	}
}