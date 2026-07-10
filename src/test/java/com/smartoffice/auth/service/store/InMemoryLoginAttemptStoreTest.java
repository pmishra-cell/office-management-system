package com.smartoffice.auth.service.store;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptStoreTest {

    @Test
    void putAndGetShouldReturnStoredSnapshot() {
        InMemoryLoginAttemptStore store = new InMemoryLoginAttemptStore();
        String key = "user@example.com";
        LoginAttemptSnapshot snapshot = new LoginAttemptSnapshot(2, Instant.now(), null);

        store.put(key, snapshot, 30_000L);

        assertThat(store.get(key)).isEqualTo(snapshot);
    }

    @Test
    void removeShouldDeleteSnapshot() {
        InMemoryLoginAttemptStore store = new InMemoryLoginAttemptStore();
        String key = "user@example.com";
        LoginAttemptSnapshot snapshot = new LoginAttemptSnapshot(1, Instant.now(), null);

        store.put(key, snapshot, 30_000L);
        store.remove(key);

        assertThat(store.get(key)).isNull();
    }

    @Test
    void expiredEntryShouldNotBeReturned() throws Exception {
        InMemoryLoginAttemptStore store = new InMemoryLoginAttemptStore();
        String key = "expiring@example.com";
        LoginAttemptSnapshot snapshot = new LoginAttemptSnapshot(1, Instant.now(), null);

        store.put(key, snapshot, 1L);
        Thread.sleep(1_100L);

        assertThat(store.get(key)).isNull();
    }
}
