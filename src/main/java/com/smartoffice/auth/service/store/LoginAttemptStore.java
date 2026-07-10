package com.smartoffice.auth.service.store;

public interface LoginAttemptStore {

    LoginAttemptSnapshot get(String key);

    void put(String key, LoginAttemptSnapshot snapshot, long ttlMs);

    void remove(String key);
}