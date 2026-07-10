package com.smartoffice.auth.service.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@ConditionalOnProperty(name = "security.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRefreshTokenRevocationStore implements RefreshTokenRevocationStore {

    private final ConcurrentMap<String, Long> revokedRefreshTokens = new ConcurrentHashMap<>();

    @Override
    public void revoke(String token, long expiresAtMs) {
        cleanupExpiredRevocations();
        revokedRefreshTokens.put(token, expiresAtMs);
    }

    @Override
    public boolean isRevoked(String token) {
        cleanupExpiredRevocations();
        return revokedRefreshTokens.containsKey(token);
    }

    private void cleanupExpiredRevocations() {
        long now = Instant.now().toEpochMilli();
        revokedRefreshTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}