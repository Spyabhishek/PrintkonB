package com.printkon.pdp.auth.models;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "jwt_blacklists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtBlacklist {

	@Id
	private String jti;

	private Instant expiresAt;
}