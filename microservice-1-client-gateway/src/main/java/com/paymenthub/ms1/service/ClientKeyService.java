package com.paymenthub.ms1.service;

import com.paymenthub.ms1.entity.ClientEncryptionKey;
import com.paymenthub.ms1.repository.ClientEncryptionKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
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
     * ════════════════════════════════════════════════════════
     * WARMUP: Load all client keys into Redis on startup
     * ════════════════════════════════════════════════════════
     */
    @PostConstruct
    public void warmupCache() {
        
        long startTime = System.nanoTime();
        
        try {
            log.info("🔥 WARMUP: Loading all client AES keys into Redis cache...");
            
            // Fetch all active client keys from database
            List<ClientEncryptionKey> allKeys = keyRepository.findAllByIsActive(true);
            
            if (allKeys.isEmpty()) {
                log.warn("⚠️ WARMUP: No active client keys found in database");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            // Load each key into Redis
            for (ClientEncryptionKey keyEntity : allKeys) {
                try {
                    String cacheKey = KEY_CACHE_PREFIX + keyEntity.getClientId();
                    
                    redisTemplate.opsForValue().set(
                        cacheKey, 
                        keyEntity.getAesKey(), 
                        Duration.ofSeconds(CACHE_TTL_SECONDS)
                    );
                    
                    successCount++;
                    log.debug("✅ Cached AES key | clientId={}", keyEntity.getClientId());
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ Failed to cache key | clientId={}", 
                            keyEntity.getClientId(), e);
                }
            }

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            
            log.info("═══════════════════════════════════════════════════════");
            log.info("🔥 WARMUP COMPLETED");
            log.info("   Total keys: {}", allKeys.size());
            log.info("   Success: {}", successCount);
            log.info("   Failed: {}", failCount);
            log.info("   Duration: {}ms", duration);
            log.info("   Cache TTL: {} hours", CACHE_TTL_SECONDS / 3600);
            log.info("═══════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("❌ WARMUP FAILED: Could not load client keys into cache", e);
        }
    }

    /**
     * Get client AES key (with Redis caching)
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
        // STEP 2: Query PostgreSQL (cache miss)
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

    /**
     * Refresh a specific client's key in cache
     */
    public void refreshCache(String clientId) {
        
        log.info("🔄 Refreshing cache | clientId={}", clientId);
        
        Optional<ClientEncryptionKey> keyEntity = 
                keyRepository.findByClientIdAndIsActive(clientId, true);
        
        if (keyEntity.isPresent()) {
            String cacheKey = KEY_CACHE_PREFIX + clientId;
            redisTemplate.opsForValue().set(
                cacheKey, 
                keyEntity.get().getAesKey(), 
                Duration.ofSeconds(CACHE_TTL_SECONDS)
            );
            log.info("✅ Cache refreshed | clientId={}", clientId);
        } else {
            log.warn("⚠️ Cannot refresh cache, client not found | clientId={}", clientId);
        }
    }
}