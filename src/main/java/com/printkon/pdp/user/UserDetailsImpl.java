package com.printkon.pdp.user;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.printkon.pdp.common.enums.AccountStatus;
import com.printkon.pdp.user.models.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsImpl implements UserDetails {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String email;

	@JsonIgnore
	private String password;

	private AccountStatus accountStatus;

	private Collection<? extends GrantedAuthority> authorities;

	public static UserDetailsImpl build(User user) {
		List<GrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole().name())).collect(Collectors.toList());

		return new UserDetailsImpl(user.getId(), user.getEmail(), user.getPassword(), user.getAccountStatus(),
				authorities);
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return accountStatus == AccountStatus.ACTIVE;
	}

}
