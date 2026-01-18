package com.printkon.pdp.auth.dto;

import java.util.List;

import com.printkon.pdp.common.enums.ERole;
import com.printkon.pdp.common.enums.RequestStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
	private String token;
	private String type = "Bearer";
	private Long id;
	private String name;
	private String email;
	private Long phone;
	private int age;
	private String gender;
	private List<ERole> roles;
	private RequestStatus upgradeRequestStatus;
	
}
