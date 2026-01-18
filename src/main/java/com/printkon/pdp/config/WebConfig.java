package com.printkon.pdp.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.printkon.pdp.security.RateLimitingFilter;

@Configuration
public class WebConfig {

	FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter(RateLimitingFilter rateLimitingFilter) {
		FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(rateLimitingFilter);
		registrationBean.addUrlPatterns("/api/*"); // Apply to all API endpoints
		registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // Execute before other filters
		return registrationBean;
	}
}