package com.smartoffice.auth.service;

import com.smartoffice.auth.api.AuthResponse;
import com.smartoffice.auth.api.LoginRequest;
import com.smartoffice.auth.api.RefreshTokenRequest;
import com.smartoffice.auth.api.RegisterRequest;
import com.smartoffice.auth.api.UserResponse;
import com.smartoffice.auth.domain.UserAccount;
import com.smartoffice.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserAccount user = new UserAccount();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        UserAccount saved = userAccountRepository.save(user);

        String accessToken = jwtTokenService.generateAccessToken(saved);
        String refreshToken = jwtTokenService.generateRefreshToken(saved);

        return new AuthResponse(accessToken, refreshToken, toUserResponse(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        loginAttemptService.assertLoginAllowed(request.email());

        UserAccount user = userAccountRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.registerFailedAttempt(request.email());
            throw new BadCredentialsException("Invalid credentials");
        }

        loginAttemptService.registerSuccessfulLogin(request.email());

        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, toUserResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtTokenService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (jwtTokenService.isRefreshTokenRevoked(refreshToken)) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        String email = jwtTokenService.parseClaims(refreshToken).getSubject();
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        String accessToken = jwtTokenService.generateAccessToken(user);
        String rotatedRefreshToken = jwtTokenService.generateRefreshToken(user);
        jwtTokenService.revokeRefreshToken(refreshToken);

        return new AuthResponse(accessToken, rotatedRefreshToken, toUserResponse(user));
    }

    public void logout(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtTokenService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        jwtTokenService.revokeRefreshToken(refreshToken);
    }

    private UserResponse toUserResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }
}
