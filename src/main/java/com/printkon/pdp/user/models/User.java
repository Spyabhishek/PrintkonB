package com.printkon.pdp.user.models;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.printkon.pdp.common.enums.AccountStatus;
import com.printkon.pdp.role.models.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, unique = true)
	private String phone;

	@Column(nullable = false)
	private int age;

	@Column(nullable = false)
	private String gender;

	@Column(nullable = false)
	private String password;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountStatus accountStatus = AccountStatus.IN_ACTIVE;

	@Builder.Default
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@Builder.Default
	@Column(name = "is_email_verified", nullable = false)
	private boolean isEmailVerified = false;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Builder.Default
	@Column(name = "created_at", updatable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at")
	private Instant updatedAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = Instant.now();
	}
}
