package com.printkon.pdp.exceptions;

public class DuplicatePhoneException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DuplicatePhoneException(String message) {
		super(message);
	}
}