package com.printkon.pdp.user.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String label; // e.g., "Home", "Office"

	private String recipientName;
	private String phone;
	private String addressLine;
	private String city;
	private String state;
	private String zip;
	private String country;

	private boolean isDefault;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
