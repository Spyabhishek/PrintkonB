package com.printkon.pdp.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class HashUtils {
	private HashUtils() {
	}

	public static String sha256(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : digest)
				sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
