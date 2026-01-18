package com.printkon.pdp.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.printkon.pdp.user.UserDetailsImpl;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Getter
public class JwtUtils {

    // --- JWT Configuration ---
    @Value("${pdp.app.secret.access.token}")
    private String secretKey;

    @Value("${app.security.jwt.access-token-ttl:15m}")
    private Duration accessTokenTtl;

    @Value("${app.security.jwt.refresh-token-ttl:14d}")
    private Duration refreshTokenTtl;

    @Value("${app.security.jwt.email-verification-ttl:15m}")
    private Duration emailVerificationTtl;

    @Value("${app.security.jwt.password-reset-ttl:10m}")
    private Duration passwordResetTtl;

    @Value("${app.security.jwt.role-upgrade-ttl:15m}")
    private Duration roleUpgradeTtl;

    // --- Cookie Configuration ---
    @Value("${app.security.cookie.domain:}")
    private String cookieDomain;

    @Value("${app.security.cookie.secure:true}")
    private boolean cookieSecure;

    // --- Brute Force Protection Configuration ---
    @Value("${app.security.brute-force.max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.brute-force.lockout-duration:15m}")
    private Duration loginLockoutDuration;

    // --- Device Security Configuration ---
    @Value("${app.security.device-hash-pepper}")
    private String deviceHashPepper;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Utils initialized with configuration:");
        log.info("  - Access Token TTL: {}", accessTokenTtl);
        log.info("  - Refresh Token TTL: {}", refreshTokenTtl);
        log.info("  - Cookie Secure: {}", cookieSecure);
        log.info("  - Max Login Attempts: {}", maxLoginAttempts);
        log.info("  - Lockout Duration: {}", loginLockoutDuration);
    }

    // --- Generic claim extraction helpers ---
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * @param token The JWT token.
     * @return The JTI string, or null if not found.
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claims == null ? null : claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired while parsing: {}", e.getMessage());
            return e.getClaims();
        } catch (Exception e) {
            log.warn("Failed to parse JWT: {}", e.getMessage());
            return null;
        }
    }

    public Instant getExpirationTime(String token) {
        Claims claims = extractAllClaims(token);
        Date expirationDate = claims.getExpiration();
        return expirationDate != null ? expirationDate.toInstant() : null;
    }

    /**
     * Calculates the remaining validity time of a token in milliseconds.
     * 
     * @param token The JWT token.
     * @return Remaining time in milliseconds, or 0 if expired or invalid.
     */
    public long getRemainingTime(String token) {
        Claims claims = extractAllClaims(token);
        if (claims == null || claims.getExpiration() == null) {
            return 0;
        }
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims empty: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    // === Token Generation Methods ===

    public String generateAccessToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        Date now = new Date();
        Date expiry = Date.from(Instant.now().plus(accessTokenTtl));
        String jti = UUID.randomUUID().toString();

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .id(jti)
                .signWith(key)
                .compact();
    }

    public String generateJwtForEmail(String email) {
        Date now = new Date();
        Date expiry = Date.from(Instant.now().plus(emailVerificationTtl));
        return Jwts.builder()
                .subject(email)
                .claim("type", "email-verification")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateJwtForPasswordReset(String email) {
        Date now = new Date();
        Date expiry = Date.from(Instant.now().plus(passwordResetTtl));
        return Jwts.builder()
                .subject(email)
                .claim("type", "password-reset")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRoleUpgradeToken(String email, Long requestId) {
        Date now = new Date();
        Date expiry = Date.from(Instant.now().plus(roleUpgradeTtl));
        return Jwts.builder()
                .subject(email)
                .claim("type", "role-upgrade")
                .claim("requestId", requestId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // === Configuration fields are accessible via Lombok @Getter ===

    // === Expiry Duration Getters (computed values - cannot use Lombok) ===
    
    public long getAccessTokenExpiryInSeconds() { return accessTokenTtl.getSeconds(); }
    public long getRefreshTokenExpiryInSeconds() { return refreshTokenTtl.getSeconds(); }
    public long getEmailVerificationExpiryInSeconds() { return emailVerificationTtl.getSeconds(); }
    public long getPasswordResetExpiryInSeconds() { return passwordResetTtl.getSeconds(); }
    public long getRoleUpgradeExpiryInSeconds() { return roleUpgradeTtl.getSeconds(); }
}