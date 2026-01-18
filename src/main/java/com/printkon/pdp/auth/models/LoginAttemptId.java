package com.printkon.pdp.auth.models;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode
public class LoginAttemptId implements Serializable {
	private static final long serialVersionUID = 1L;
	private String identifier;
	private String remoteAddr;
}