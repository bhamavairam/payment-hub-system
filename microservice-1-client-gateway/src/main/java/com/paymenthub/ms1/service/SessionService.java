package com.paymenthub.ms1.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
@Slf4j
public class SessionService {

    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt-expiration-seconds}")
    private long jwtExpirationSeconds;

    private Key signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        log.info("🔐 JWT Service Initialized | Expiration={}s", jwtExpirationSeconds);
    }

    /**
     * Step 1: Create JWT token instead of Redis session
     */
    public String createSession(String clientId) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationSeconds * 1000);

        String token = Jwts.builder()
                .setSubject(clientId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        log.info("✅ JWT created | clientId={} | expiresIn={}s",
                clientId, jwtExpirationSeconds);

        return token;
    }

    /**
     * Validate JWT token
     */
    public String validateSession(String authorizationHeader) {

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("❌ Missing Authorization header");
            throw new RuntimeException("Missing Authorization header");
        }

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7).trim()
                : authorizationHeader.trim();

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String clientId = claims.getSubject();

            log.debug("✅ JWT valid | clientId={}", clientId);
            return clientId;

        } catch (ExpiredJwtException e) {
            log.warn("❌ JWT expired");
            throw new RuntimeException("Token expired. Call /auth/token again.");

        } catch (JwtException e) {
            log.warn("❌ Invalid JWT");
            throw new RuntimeException("Invalid token");
        }
    }

    /**
     * Logout (JWT is stateless — nothing to delete)
     */
    public void deleteSession(String authorizationHeader) {
        log.info("🚪 JWT logout requested (stateless)");
    }

    /**
     * Validate credentials (same as before)
     */
    public boolean validateCredentials(String clientId, String clientSecret) {

        boolean valid = clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();

        if (!valid) {
            log.warn("❌ Invalid credentials | clientId={}", clientId);
        }

        return valid;
    }
}