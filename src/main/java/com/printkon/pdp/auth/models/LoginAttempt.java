package com.printkon.pdp.auth.models;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "login_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(LoginAttemptId.class)
public class LoginAttempt {

	@Id
	private String identifier;

	@Id
	private String remoteAddr;

	private int attempts;

	private Instant lockedUntil;

	public LoginAttempt(String identifier, String remoteAddr) {
		this.identifier = identifier;
		this.remoteAddr = remoteAddr;
		this.attempts = 0;
	}

}