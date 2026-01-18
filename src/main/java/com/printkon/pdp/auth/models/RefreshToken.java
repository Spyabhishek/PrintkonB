package com.printkon.pdp.auth.models;

import java.time.Instant;

import com.printkon.pdp.user.models.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

	@Id
	private String id; // hashed token ID

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Builder.Default
	@Column(nullable = false)
	private boolean revoked = false;

	@Column(name = "replaced_by")
	private String replacedBy;

	// Hashed device signature
	@Column(name = "device_info")
	private String deviceInfo;

	// Raw frontend-provided device UUID
	@Column(name = "device_id", length = 128)
	private String deviceId;

	// Raw User-Agent string
	@Column(name = "user_agent", length = 512)
	private String userAgent;

	// IP address at login
	@Column(name = "ip_address", length = 45)
	private String ipAddress;
}
