package com.smartoffice.auth.service;

import com.smartoffice.auth.domain.Role;
import com.smartoffice.auth.domain.UserAccount;
import com.smartoffice.auth.service.store.RefreshTokenRevocationStore;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {

    private static final String SECRET = "this-is-a-test-secret-with-at-least-32-bytes-long";

    private RefreshTokenRevocationStore refreshTokenRevocationStore;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenRevocationStore = mock(RefreshTokenRevocationStore.class);
        jwtTokenService = new JwtTokenService(SECRET, 60_000L, 120_000L, refreshTokenRevocationStore);
    }

    @Test
    void generateAccessTokenShouldSetAccessTypeClaim() {
        UserAccount user = testUser();

        String accessToken = jwtTokenService.generateAccessToken(user);
        Claims claims = jwtTokenService.parseClaims(accessToken);

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("role", String.class)).isEqualTo("EMPLOYEE");
        assertThat(claims.get("userId", Long.class)).isEqualTo(101L);
    }

    @Test
    void generateRefreshTokenShouldBeDetectedAsRefreshToken() {
        UserAccount user = testUser();

        String refreshToken = jwtTokenService.generateRefreshToken(user);

        assertThat(jwtTokenService.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    void accessTokenShouldNotBeDetectedAsRefreshToken() {
        UserAccount user = testUser();

        String accessToken = jwtTokenService.generateAccessToken(user);

        assertThat(jwtTokenService.isRefreshToken(accessToken)).isFalse();
    }

    @Test
    void invalidTokenShouldNotBeDetectedAsRefreshToken() {
        assertThat(jwtTokenService.isRefreshToken("bad.token.value")).isFalse();
    }

    @Test
    void revokeRefreshTokenShouldDelegateToStore() {
        UserAccount user = testUser();
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        jwtTokenService.revokeRefreshToken(refreshToken);

        verify(refreshTokenRevocationStore).revoke(anyString(), anyLong());
    }

    @Test
    void revokeRefreshTokenShouldIgnoreAccessToken() {
        UserAccount user = testUser();
        String accessToken = jwtTokenService.generateAccessToken(user);

        jwtTokenService.revokeRefreshToken(accessToken);

        verify(refreshTokenRevocationStore, never()).revoke(anyString(), anyLong());
    }

    @Test
    void isRefreshTokenRevokedShouldDelegateToStore() {
        String token = "refresh-token-value";
        when(refreshTokenRevocationStore.isRevoked(token)).thenReturn(true);

        boolean revoked = jwtTokenService.isRefreshTokenRevoked(token);

        assertThat(revoked).isTrue();
        verify(refreshTokenRevocationStore).isRevoked(token);
    }

    private UserAccount testUser() {
        UserAccount user = new UserAccount();
        user.setId(101L);
        user.setEmail("user@example.com");
        user.setName("User");
        user.setRole(Role.EMPLOYEE);
        return user;
    }
}
