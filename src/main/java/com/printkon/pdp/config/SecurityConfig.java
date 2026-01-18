package com.printkon.pdp.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.printkon.pdp.security.AuthEntryPointJwt;
import com.printkon.pdp.security.AuthTokenFilter;
import com.printkon.pdp.security.JwtUtils;
import com.printkon.pdp.user.UserServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final AuthEntryPointJwt unauthorizedHandler;
	private final JwtUtils jwtUtils;
	private final UserServiceImpl userService;
	private final Environment environment;
	private final CorsProperties corsProperties;

	public SecurityConfig(AuthEntryPointJwt unauthorizedHandler, JwtUtils jwtUtils, UserServiceImpl userService,
			Environment environment, CorsProperties corsProperties) {
		this.unauthorizedHandler = unauthorizedHandler;
		this.jwtUtils = jwtUtils;
		this.userService = userService;
		this.environment = environment;
		this.corsProperties = corsProperties;
	}

	@Bean
	AuthTokenFilter authenticationJwtTokenFilter() {
		return new AuthTokenFilter(jwtUtils, userService);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// Modern way to check profiles using Profiles.of()
		boolean isDevelopment = environment.acceptsProfiles(Profiles.of("dev"));
		boolean isTest = environment.acceptsProfiles(Profiles.of("test"));
		boolean isProduction = environment.acceptsProfiles(Profiles.of("prod"));

		return http.cors(cors -> cors.configurationSource(request -> {
			var corsConfig = new CorsConfiguration();

			// Use configuration from application.yml
			corsConfig.setAllowedOrigins(corsProperties.getAllowedOrigins());
			corsConfig.setAllowedMethods(corsProperties.getAllowedMethods());
			corsConfig.setAllowedHeaders(corsProperties.getAllowedHeaders());
			corsConfig.setAllowCredentials(corsProperties.isAllowCredentials());
			corsConfig.setMaxAge(corsProperties.getMaxAge());

			// Expose headers for JWT tokens
			corsConfig.setExposedHeaders(List.of("Set-Cookie", "Authorization"));

			return corsConfig;
		})).csrf(csrf -> csrf.disable())
				.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> {

					// Always allow auth endpoints
					auth.requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/refresh",
							"/api/auth/activate", "/api/auth/reset-password", "/api/auth/forgot-password").permitAll();

					// Public product read endpoints (no authentication required)
					// Public read endpoints for catalogs (Products & Categories)
					auth.requestMatchers(HttpMethod.GET, "/api/products", // Get all products
							"/api/products/*", // Get product by ID
							"/api/products/search", // Search products
							"/api/products/search/simple",
							"/api/categories", // Get all categories (FIX)
							"/api/categories/*", // Get category by ID (FIX)
							"/api/categories/*/products" // Get products by category (FIX)
					).permitAll();

					auth.requestMatchers(HttpMethod.GET, "/api/landing/stats", "/api/landing/printing-services",
							"/api/landing/testimonials", "/api/landing/features").permitAll();

					// Public image access
					auth.requestMatchers(HttpMethod.GET, "/api/images/products/**", "/api/images/categories/**")
							.permitAll();

					// Profile-specific actuator security
					if (isDevelopment || isTest) {
						// DEV/TEST: Allow all actuator endpoints without authentication
						auth.requestMatchers("/actuator/**").permitAll();
						// Allow H2 console in dev/test
						auth.requestMatchers("/h2-console/**").permitAll();
					} else if (isProduction) {
						// PROD: Health endpoint for load balancers, others require ADMIN role
						auth.requestMatchers("/actuator/health").permitAll().requestMatchers("/actuator/**")
								.hasRole("ADMIN");
					} else {
						// Default: Require authentication for actuator
						auth.requestMatchers("/actuator/health").permitAll().requestMatchers("/actuator/**")
								.authenticated();
					}

					// All other endpoints require authentication
					auth.anyRequest().authenticated();
				}).addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class).build();
	}
}