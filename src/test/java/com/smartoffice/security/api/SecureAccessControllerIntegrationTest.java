package com.smartoffice.security.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoffice.auth.domain.Role;
import com.smartoffice.auth.domain.UserAccount;
import com.smartoffice.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecureAccessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        if (userAccountRepository.findByEmail("admin@example.com").isEmpty()) {
            UserAccount admin = new UserAccount();
            admin.setEmail("admin@example.com");
            admin.setName("Admin");
            admin.setRole(Role.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("AdminPass123!"));
            userAccountRepository.save(admin);
        }
    }

    @Test
    void employeeTokenShouldBeForbiddenForAdminEndpoint() throws Exception {
        String registerPayload = """
                {
                  "email": "rbac.employee@example.com",
                  "name": "Rbac Employee",
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

        String employeeAccessToken = objectMapper.readTree(registerResponse).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/secure/employee")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee area access granted"));

        mockMvc.perform(get("/api/v1/secure/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenShouldAccessAdminEndpoint() throws Exception {
        String loginPayload = """
                {
                  "email": "admin@example.com",
                  "password": "AdminPass123!"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String adminAccessToken = loginJson.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/secure/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin area access granted"));
    }
}