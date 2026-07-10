package com.smartoffice.auth.service;

import com.smartoffice.auth.service.store.LoginAttemptSnapshot;
import com.smartoffice.auth.service.store.LoginAttemptStore;
import com.smartoffice.common.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class LoginAttemptService {

    private final int maxFailedAttempts;
    private final long attemptWindowMs;
    private final long lockoutMs;
    private final LoginAttemptStore loginAttemptStore;

    public LoginAttemptService(
            @Value("${security.login.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${security.login.attempt-window-ms:900000}") long attemptWindowMs,
            @Value("${security.login.lockout-ms:900000}") long lockoutMs,
            LoginAttemptStore loginAttemptStore
    ) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.attemptWindowMs = attemptWindowMs;
        this.lockoutMs = lockoutMs;
        this.loginAttemptStore = loginAttemptStore;
    }

    public void assertLoginAllowed(String key) {
        String normalizedKey = normalizeKey(key);
        Instant now = Instant.now();
        LoginAttemptSnapshot state = loginAttemptStore.get(normalizedKey);
        if (state == null) {
            return;
        }

        if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
            long minutesLeft = Math.max(1, Duration.between(now, state.lockedUntil()).toMinutes());
            throw new TooManyRequestsException("Too many failed login attempts. Try again in " + minutesLeft + " minute(s)");
        }

        if (state.lockedUntil() != null && !state.lockedUntil().isAfter(now)) {
            loginAttemptStore.remove(normalizedKey);
        }
    }

    public void registerFailedAttempt(String key) {
        String normalizedKey = normalizeKey(key);
        Instant now = Instant.now();
        LoginAttemptSnapshot current = loginAttemptStore.get(normalizedKey);

        if (current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(now)) {
            return;
        }

        int failedAttempts;
        Instant firstFailureAt;
        Instant lockedUntil = null;

        if (current == null || current.firstFailureAt() == null
                || Duration.between(current.firstFailureAt(), now).toMillis() > attemptWindowMs) {
            failedAttempts = 1;
            firstFailureAt = now;
        } else {
            failedAttempts = current.failedAttempts() + 1;
            firstFailureAt = current.firstFailureAt();
        }

        if (failedAttempts >= maxFailedAttempts) {
            lockedUntil = now.plusMillis(lockoutMs);
        }

        long ttlMs = Math.max(attemptWindowMs, lockoutMs) + attemptWindowMs;
        loginAttemptStore.put(normalizedKey, new LoginAttemptSnapshot(failedAttempts, firstFailureAt, lockedUntil), ttlMs);
    }

    public void registerSuccessfulLogin(String key) {
        loginAttemptStore.remove(normalizeKey(key));
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}