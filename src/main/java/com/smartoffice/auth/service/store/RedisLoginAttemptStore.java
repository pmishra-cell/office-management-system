package com.smartoffice.auth.service.store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "security.redis.enabled", havingValue = "true")
public class RedisLoginAttemptStore implements LoginAttemptStore {

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisLoginAttemptStore(
            StringRedisTemplate redisTemplate,
            @Value("${security.redis.key-prefix:smartoffice:security}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public LoginAttemptSnapshot get(String key) {
        String raw = redisTemplate.opsForValue().get(redisKey(key));
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] parts = raw.split("\\|", -1);
        if (parts.length != 3) {
            return null;
        }

        try {
            int failedAttempts = Integer.parseInt(parts[0]);
            Instant firstFailureAt = Instant.ofEpochMilli(Long.parseLong(parts[1]));
            Instant lockedUntil = "-".equals(parts[2]) ? null : Instant.ofEpochMilli(Long.parseLong(parts[2]));
            return new LoginAttemptSnapshot(failedAttempts, firstFailureAt, lockedUntil);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public void put(String key, LoginAttemptSnapshot snapshot, long ttlMs) {
        String serialized = snapshot.failedAttempts()
                + "|" + snapshot.firstFailureAt().toEpochMilli()
                + "|" + (snapshot.lockedUntil() == null ? "-" : snapshot.lockedUntil().toEpochMilli());

        redisTemplate.opsForValue().set(
                redisKey(key),
                serialized,
                Duration.ofMillis(Math.max(1000L, ttlMs))
        );
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete(redisKey(key));
    }

    private String redisKey(String key) {
        return keyPrefix + ":login-attempts:" + key;
    }
}