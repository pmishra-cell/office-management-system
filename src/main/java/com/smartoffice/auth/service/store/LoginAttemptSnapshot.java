package com.smartoffice.auth.service.store;

import java.time.Instant;

public record LoginAttemptSnapshot(
        int failedAttempts,
        Instant firstFailureAt,
        Instant lockedUntil
) {
}