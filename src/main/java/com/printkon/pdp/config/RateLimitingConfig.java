package com.printkon.pdp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

	@Bean
	Map<String, Bucket> rateLimitBuckets() {
		return new ConcurrentHashMap<>();
	}

	// Different rate limits for different endpoints
	public static class RateLimits {
		public static final Bandwidth PRODUCT_READ_LIMIT = Bandwidth.classic(100,
				Refill.intervally(100, Duration.ofMinutes(1))); // 100 requests per minute
		public static final Bandwidth SEARCH_LIMIT = Bandwidth.classic(30,
				Refill.intervally(30, Duration.ofMinutes(1))); // 30 searches per minute
		public static final Bandwidth GENERAL_LIMIT = Bandwidth.classic(200,
				Refill.intervally(200, Duration.ofMinutes(1))); // 200 requests per minute
	}
}