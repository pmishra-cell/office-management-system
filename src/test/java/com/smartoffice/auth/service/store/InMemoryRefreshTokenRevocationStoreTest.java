package com.smartoffice.auth.service.store;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRefreshTokenRevocationStoreTest {

    @Test
    void revokeShouldMarkTokenAsRevoked() {
        InMemoryRefreshTokenRevocationStore store = new InMemoryRefreshTokenRevocationStore();
        String token = "refresh-token";
        long expiresAtMs = Instant.now().plusSeconds(60).toEpochMilli();

        store.revoke(token, expiresAtMs);

        assertThat(store.isRevoked(token)).isTrue();
    }

    @Test
    void expiredRevokedTokenShouldBeCleanedUp() {
        InMemoryRefreshTokenRevocationStore store = new InMemoryRefreshTokenRevocationStore();
        String token = "expired-refresh-token";
        long expiresAtMs = Instant.now().minusSeconds(1).toEpochMilli();

        store.revoke(token, expiresAtMs);

        assertThat(store.isRevoked(token)).isFalse();
    }
}
