package com.printkon.pdp.role.models;

import java.time.LocalDateTime;

import com.printkon.pdp.common.enums.RequestStatus;
import com.printkon.pdp.user.models.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_upgrade_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpgradeRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String companyName;

	@Column(nullable = false, unique = true)
	private String gstNumber;

	@Column(nullable = false, length = 500)
	private String customNote;

	@Column(nullable = false)
	private LocalDateTime requestDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RequestStatus status = RequestStatus.NONE;
}
