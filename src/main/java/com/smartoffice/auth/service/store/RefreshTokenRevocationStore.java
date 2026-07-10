package com.smartoffice.auth.service.store;

public interface RefreshTokenRevocationStore {

    void revoke(String token, long expiresAtMs);

    boolean isRevoked(String token);
}