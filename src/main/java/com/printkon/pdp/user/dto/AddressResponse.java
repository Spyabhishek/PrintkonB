package com.printkon.pdp.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
	private Long id;
	private String label;
	private String recipientName;
	private String phone;
	private String addressLine;
	private String city;
	private String state;
	private String zip;
	private String country;
	private Boolean isDefault;
	private Long userId;
}