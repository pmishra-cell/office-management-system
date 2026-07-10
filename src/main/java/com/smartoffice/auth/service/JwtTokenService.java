package com.smartoffice.auth.service;

import com.smartoffice.auth.domain.UserAccount;
import com.smartoffice.auth.service.store.RefreshTokenRevocationStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final RefreshTokenRevocationStore refreshTokenRevocationStore;

    public JwtTokenService(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
                           @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
                           RefreshTokenRevocationStore refreshTokenRevocationStore) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshTokenRevocationStore = refreshTokenRevocationStore;
    }

    public String generateAccessToken(UserAccount user) {
        return generateToken(user, accessTokenExpirationMs, Map.of("type", "access"));
    }

    public String generateRefreshToken(UserAccount user) {
        return generateToken(user, refreshTokenExpirationMs, Map.of("type", "refresh"));
    }

    private String generateToken(UserAccount user, long expirationMs, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public void revokeRefreshToken(String token) {
        Claims claims = parseClaims(token);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            return;
        }
        Date expiration = claims.getExpiration();
        long expiresAtMs = expiration != null ? expiration.getTime() : Instant.now().toEpochMilli();
        refreshTokenRevocationStore.revoke(token, expiresAtMs);
    }

    public boolean isRefreshTokenRevoked(String token) {
        return refreshTokenRevocationStore.isRevoked(token);
    }
}
