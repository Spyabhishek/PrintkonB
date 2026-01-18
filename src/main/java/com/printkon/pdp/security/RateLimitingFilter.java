package com.printkon.pdp.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printkon.pdp.config.RateLimitProperties;
import com.printkon.pdp.common.dto.ResponseStructure;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

	private final RateLimitProperties rateLimitProperties;
	private final Environment environment;
	private final ObjectMapper objectMapper = new ObjectMapper();

	// Store buckets per IP address and endpoint type
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	// Track suspicious IPs (IPs that hit rate limits frequently)
	private final Map<String, Integer> suspiciousIps = new ConcurrentHashMap<>();
	private final Map<String, Long> ipBlacklist = new ConcurrentHashMap<>();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Skip rate limiting in development/test profiles
		if (environment.acceptsProfiles(Profiles.of("dev", "test")) || !rateLimitProperties.isEnabled()) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientIp = getClientIpAddress(request);
		String requestPath = request.getRequestURI();
		String method = request.getMethod();

		// Check if IP is blacklisted (temporarily blocked)
		if (isIpBlacklisted(clientIp)) {
			log.warn("Blocked request from blacklisted IP: {} to {}", clientIp, requestPath);
			sendRateLimitResponse(response, "IP temporarily blocked due to suspicious activity");
			return;
		}

		// Determine rate limit based on endpoint
		Bucket bucket = resolveBucket(clientIp, requestPath, method);

		if (bucket.tryConsume(1)) {
			// Request allowed
			filterChain.doFilter(request, response);
		} else {
			// Rate limit exceeded
			log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestPath);

			// Track suspicious activity
			trackSuspiciousActivity(clientIp);

			sendRateLimitResponse(response, "Rate limit exceeded. Please try again later.");
		}
	}

	private Bucket resolveBucket(String clientIp, String requestPath, String method) {
		String bucketKey = clientIp + ":" + getBucketType(requestPath, method);

		return buckets.computeIfAbsent(bucketKey, key -> {
			Bandwidth bandwidth = getBandwidthForRequest(requestPath, method);
			return Bucket.builder().addLimit(bandwidth).build();
		});
	}

	private String getBucketType(String requestPath, String method) {
		if ("GET".equals(method) && requestPath.startsWith("/api/products")) {
			if (requestPath.contains("/search")) {
				return "SEARCH";
			}
			return "PRODUCT_READ";
		}
		return "GENERAL";
	}

	private Bandwidth getBandwidthForRequest(String requestPath, String method) {
		if ("GET".equals(method) && requestPath.startsWith("/api/products")) {
			if (requestPath.contains("/search")) {
				// More restrictive for search (CPU intensive)
				return Bandwidth.classic(rateLimitProperties.getSearchLimit(),
						Refill.intervally(rateLimitProperties.getSearchLimit(), rateLimitProperties.getWindow()));
			}
			// Product read endpoints
			return Bandwidth.classic(rateLimitProperties.getProductReadLimit(),
					Refill.intervally(rateLimitProperties.getProductReadLimit(), rateLimitProperties.getWindow()));
		}

		// General endpoints
		return Bandwidth.classic(rateLimitProperties.getGeneralLimit(),
				Refill.intervally(rateLimitProperties.getGeneralLimit(), rateLimitProperties.getWindow()));
	}

	private void trackSuspiciousActivity(String clientIp) {
		int violations = suspiciousIps.merge(clientIp, 1, Integer::sum);

		// If IP hits rate limit 5 times within the tracking period, blacklist
		// temporarily
		if (violations >= 5) {
			long blacklistUntil = System.currentTimeMillis() + Duration.ofMinutes(15).toMillis();
			ipBlacklist.put(clientIp, blacklistUntil);
			suspiciousIps.remove(clientIp);
			log.error("IP {} blacklisted for 15 minutes due to repeated rate limit violations", clientIp);
		}
	}

	private boolean isIpBlacklisted(String clientIp) {
		Long blacklistUntil = ipBlacklist.get(clientIp);
		if (blacklistUntil != null) {
			if (System.currentTimeMillis() > blacklistUntil) {
				// Blacklist period expired
				ipBlacklist.remove(clientIp);
				return false;
			}
			return true;
		}
		return false;
	}

	private String getClientIpAddress(HttpServletRequest request) {
		// Check various headers for the real client IP (in case of proxies/load
		// balancers)
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
			// X-Forwarded-For can contain multiple IPs, get the first one
			return xForwardedFor.split(",")[0].trim();
		}

		String xRealIp = request.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
			return xRealIp;
		}

		return request.getRemoteAddr();
	}

	private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		ResponseStructure<Object> errorResponse = ResponseStructure.builder()
				.statusCode(HttpStatus.TOO_MANY_REQUESTS.value()).message(message).data(null)
				.timestamp(LocalDateTime.now()).build();

		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
		response.getWriter().flush();
	}

	// Cleanup method to periodically clean up old entries (can be scheduled)
	public void cleanupOldEntries() {
		// Clean up expired blacklist entries
		long currentTime = System.currentTimeMillis();
		ipBlacklist.entrySet().removeIf(entry -> currentTime > entry.getValue());

		// Clean up old suspicious IP entries (reset every hour)
		if (currentTime % (60 * 60 * 1000) < 60 * 1000) { // Roughly every hour
			suspiciousIps.clear();
		}
	}
}