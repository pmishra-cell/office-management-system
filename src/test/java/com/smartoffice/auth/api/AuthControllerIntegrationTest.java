package com.smartoffice.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAndLoginFlowShouldReturnTokens() throws Exception {
        String registerPayload = """
                {
                  "email": "alice@example.com",
                  "name": "Alice",
                  "password": "StrongPass123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));

        String loginPayload = """
                {
                  "email": "alice@example.com",
                  "password": "StrongPass123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());
    }

    @Test
    void refreshAndLogoutFlowShouldRotateAndRevokeRefreshTokens() throws Exception {
        String registerPayload = """
                {
                  "email": "bob@example.com",
                  "name": "Bob",
                  "password": "StrongPass123!"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String firstRefreshToken = registerJson.get("refreshToken").asText();

        String refreshPayload = objectMapper.writeValueAsString(
                new RefreshTokenRequest(firstRefreshToken)
        );

        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondRefreshToken = objectMapper.readTree(refreshResponse).get("refreshToken").asText();

        String logoutPayload = objectMapper.writeValueAsString(
                new RefreshTokenRequest(secondRefreshToken)
        );

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldLockAfterRepeatedFailures() throws Exception {
        String registerPayload = """
                {
                  "email": "lockout@example.com",
                  "name": "Lockout User",
                  "password": "StrongPass123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated());

        String badLoginPayload = """
                {
                  "email": "lockout@example.com",
                  "password": "WrongPassword!"
                }
                """;

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badLoginPayload))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("Too many failed login attempts")));

        String validLoginPayload = """
                {
                  "email": "lockout@example.com",
                  "password": "StrongPass123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLoginPayload))
                .andExpect(status().isTooManyRequests());
    }
}
