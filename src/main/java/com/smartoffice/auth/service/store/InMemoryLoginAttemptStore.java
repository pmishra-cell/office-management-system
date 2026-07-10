package com.smartoffice.auth.service.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@ConditionalOnProperty(name = "security.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryLoginAttemptStore implements LoginAttemptStore {

    private final ConcurrentMap<String, Entry> attempts = new ConcurrentHashMap<>();

    @Override
    public LoginAttemptSnapshot get(String key) {
        cleanupExpired();
        Entry entry = attempts.get(key);
        return entry == null ? null : entry.snapshot;
    }

    @Override
    public void put(String key, LoginAttemptSnapshot snapshot, long ttlMs) {
        cleanupExpired();
        long expiresAtMs = Instant.now().toEpochMilli() + Math.max(1000L, ttlMs);
        attempts.put(key, new Entry(snapshot, expiresAtMs));
    }

    @Override
    public void remove(String key) {
        attempts.remove(key);
    }

    private void cleanupExpired() {
        long now = Instant.now().toEpochMilli();
        attempts.entrySet().removeIf(entry -> entry.getValue().expiresAtMs <= now);
    }

    private record Entry(LoginAttemptSnapshot snapshot, long expiresAtMs) {
    }
}