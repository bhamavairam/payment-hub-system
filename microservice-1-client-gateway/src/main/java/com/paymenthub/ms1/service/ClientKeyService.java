package com.paymenthub.ms1.service;

import com.paymenthub.ms1.entity.ClientEncryptionKey;
import com.paymenthub.ms1.repository.ClientEncryptionKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientKeyService {

    private final ClientEncryptionKeyRepository keyRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String KEY_CACHE_PREFIX = "client:aes:";
    private static final long CACHE_TTL_SECONDS = 86400; // 24 hours

    /**
     * Get client AES key (with Redis caching)
     * 
     * Flow:
     * 1. Check Redis cache
     * 2. If not in cache, query PostgreSQL
     * 3. Cache in Redis for 24 hours
     * 4. Return AES key
     */
    public String getClientAesKey(String clientId) {
        
        long startTime = System.nanoTime();
        
        // ════════════════════════════════════════════════════
        // STEP 1: Check Redis cache
        // ════════════════════════════════════════════════════
        String cacheKey = KEY_CACHE_PREFIX + clientId;
        String cachedKey = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedKey != null) {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            log.debug("✅ AES key found in cache | clientId={} | {}ms", clientId, duration);
            return cachedKey;
        }

        // ════════════════════════════════════════════════════
        // STEP 2: Query PostgreSQL
        // ════════════════════════════════════════════════════
        log.debug("🔍 AES key not in cache, querying DB | clientId={}", clientId);
        
        Optional<ClientEncryptionKey> keyEntity = 
                keyRepository.findByClientIdAndIsActive(clientId, true);
        
        if (keyEntity.isEmpty()) {
            log.error("❌ No active AES key found for client | clientId={}", clientId);
            throw new RuntimeException("Client AES key not found: " + clientId);
        }

        ClientEncryptionKey key = keyEntity.get();
        String aesKey = key.getAesKey();

        // ════════════════════════════════════════════════════
        // STEP 3: Cache in Redis
        // ════════════════════════════════════════════════════
        redisTemplate.opsForValue().set(
            cacheKey, 
            aesKey, 
            Duration.ofSeconds(CACHE_TTL_SECONDS)
        );
        
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        log.info("💾 AES key loaded from DB and cached | clientId={} | {}ms", clientId, duration);

        return aesKey;
    }

    /**
     * Invalidate cache (for key rotation)
     */
    public void invalidateCache(String clientId) {
        String cacheKey = KEY_CACHE_PREFIX + clientId;
        redisTemplate.delete(cacheKey);
        log.info("🗑️ Cache invalidated | clientId={}", clientId);
    }
}