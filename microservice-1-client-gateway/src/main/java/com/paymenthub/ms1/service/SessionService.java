package com.paymenthub.ms1.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class SessionService {

    private static final String SESSION_PREFIX = "session:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${app.session-ttl-seconds:86400}")
    private long sessionTtlSeconds;

    /**
     * Step 1: Client calls /auth/token
     * We create a sessionId, store it in Redis for 24 hours
     * Return sessionId to client
     */
    public String createSession(String clientId) {
    	System.out.println("Start printing");
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
            SESSION_PREFIX + sessionId,
            clientId,
            Duration.ofSeconds(sessionTtlSeconds)
        );
        log.info("Session created | clientId={}", clientId);
        return sessionId;
    }

    /**
     * Step 2: Every transaction request calls this
     * Checks if the sessionId exists in Redis
     * Returns clientId if valid, throws exception if not
     */
    public String validateSession(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new RuntimeException("Missing Authorization header");
        }

        // Remove "Bearer " prefix if present
        String sessionId = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7).trim()
                : authorizationHeader.trim();

        String clientId = redisTemplate.opsForValue()
                .get(SESSION_PREFIX + sessionId);

        if (clientId == null) {
            throw new RuntimeException(
                "Invalid or expired session. Call /api/v1/auth/token first.");
        }

        log.debug("Session valid | clientId={}", clientId);
        return clientId;
    }

    /**
     * Logout - deletes session from Redis immediately
     */
    public void deleteSession(String authorizationHeader) {
        String sessionId = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7).trim()
                : authorizationHeader.trim();
        redisTemplate.delete(SESSION_PREFIX + sessionId);
        log.info("Session deleted | sessionId={}", sessionId);
    }

    /**
     * Validate credentials before creating session.
     * TODO: Replace with real DB check when you have a clients table.
     * For now any non-empty values pass.
     */
    public boolean validateCredentials(String clientId, String clientSecret) {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }
}