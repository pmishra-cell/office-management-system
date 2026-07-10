package com.smartoffice.auth.service.store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "security.redis.enabled", havingValue = "true")
public class RedisRefreshTokenRevocationStore implements RefreshTokenRevocationStore {

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisRefreshTokenRevocationStore(
            StringRedisTemplate redisTemplate,
            @Value("${security.redis.key-prefix:smartoffice:security}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public void revoke(String token, long expiresAtMs) {
        long ttlMs = Math.max(1000L, expiresAtMs - Instant.now().toEpochMilli());
        String key = keyPrefix + ":revoked-refresh:" + token;
        redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(ttlMs));
    }

    @Override
    public boolean isRevoked(String token) {
        String key = keyPrefix + ":revoked-refresh:" + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}