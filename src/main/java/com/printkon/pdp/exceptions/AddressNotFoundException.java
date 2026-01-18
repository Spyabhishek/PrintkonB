package com.printkon.pdp.exceptions;

public class AddressNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AddressNotFoundException(String message) {
		super(message);
	}

	public AddressNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}