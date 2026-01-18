package com.printkon.pdp.order.models;

import jakarta.persistence.Embeddable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class ShippingAddress {
	private String recipientName;
	private String phone;
	private String addressLine;
	private String city;
	private String state;
	private String zip;
	private String country;
}
