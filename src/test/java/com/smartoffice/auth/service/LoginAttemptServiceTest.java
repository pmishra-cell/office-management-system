package com.smartoffice.auth.service;

import com.smartoffice.auth.service.store.InMemoryLoginAttemptStore;
import com.smartoffice.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService(
                3,
                300_000L,
                300_000L,
                new InMemoryLoginAttemptStore()
        );
    }

    @Test
    void shouldAllowLoginForNewIdentity() {
        assertThatCode(() -> loginAttemptService.assertLoginAllowed("new.user@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldLockAfterMaxFailures() {
        String key = "locked.user@example.com";

        loginAttemptService.registerFailedAttempt(key);
        loginAttemptService.registerFailedAttempt(key);
        loginAttemptService.registerFailedAttempt(key);

        assertThatThrownBy(() -> loginAttemptService.assertLoginAllowed(key))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void successfulLoginShouldClearFailures() {
        String key = "success.user@example.com";

        loginAttemptService.registerFailedAttempt(key);
        loginAttemptService.registerFailedAttempt(key);
        loginAttemptService.registerSuccessfulLogin(key);

        assertThatCode(() -> loginAttemptService.assertLoginAllowed(key))
                .doesNotThrowAnyException();
    }

    @Test
    void keyNormalizationShouldTreatCaseAndWhitespaceAsSameIdentity() {
        loginAttemptService.registerFailedAttempt("  MIXED.CASE@EXAMPLE.COM ");
        loginAttemptService.registerFailedAttempt("mixed.case@example.com");
        loginAttemptService.registerFailedAttempt("mixed.case@example.com");

        assertThatThrownBy(() -> loginAttemptService.assertLoginAllowed(" mixed.case@example.com "))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
