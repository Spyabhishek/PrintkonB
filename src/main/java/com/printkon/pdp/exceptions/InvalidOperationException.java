package com.printkon.pdp.exceptions;

public class InvalidOperationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidOperationException(String message) {
		super(message);
	}

	public InvalidOperationException(String message, Throwable cause) {
		super(message, cause);
	}
}