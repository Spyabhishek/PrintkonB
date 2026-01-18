package com.printkon.pdp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.printkon.pdp.user.UserServiceImpl;

import java.io.IOException;

@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final UserServiceImpl userService;

	public AuthTokenFilter(JwtUtils jwtUtils, UserServiceImpl userService) {
		this.jwtUtils = jwtUtils;
		this.userService = userService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Allow CORS preflight requests to pass through
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		try {
			String jwt = parseJwt(request);

			// Only process authentication if not already set
			if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				boolean isValid = jwtUtils.validateJwtToken(jwt);

				if (isValid) {
					String username = jwtUtils.extractUsername(jwt);
					UserDetails userDetails = userService.loadUserByUsername(username);

					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				} else {
					// Set custom error message for the AuthenticationEntryPoint
					request.setAttribute("jwtException", "Invalid or expired JWT token.");
				}
			}
		} catch (Exception e) {
			log.error("Authentication failed for request to {} from IP {}: {}", request.getRequestURI(),
					request.getRemoteAddr(), e.getMessage());
			request.setAttribute("jwtException", e.getMessage());
		}

		filterChain.doFilter(request, response);
	}

	private String parseJwt(HttpServletRequest request) {
	    String headerAuth = request.getHeader("Authorization");
	    if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
	        return headerAuth.substring(7);
	    }

	    if (request.getCookies() != null) {
	        for (Cookie cookie : request.getCookies()) {
	            if ("jwt".equals(cookie.getName())) { 
	                return cookie.getValue();
	            }
	        }
	    }

	    return null; // No token found
	}

}
