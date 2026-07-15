package com.smartoffice.hr.api;

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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HrModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setupAdmin() {
        if (userAccountRepository.findByEmail("hr-admin@example.com").isEmpty()) {
            UserAccount admin = new UserAccount();
            admin.setEmail("hr-admin@example.com");
            admin.setName("HR Admin");
            admin.setRole(Role.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("AdminPass123!"));
            userAccountRepository.save(admin);
        }
    }

    @Test
    void adminShouldCreateEmployeeMarkAttendanceAndApproveLeave() throws Exception {
        String adminToken = adminToken();

        String createEmployeePayload = """
                {
                  "name": "John Worker",
                  "email": "john.worker@example.com",
                  "department": "Engineering",
                  "title": "Backend Engineer"
                }
                """;

        String employeeResponse = mockMvc.perform(post("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("john.worker@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long employeeId = objectMapper.readTree(employeeResponse).get("id").asLong();

        String attendancePayload = objectMapper.writeValueAsString(java.util.Map.of(
                "employeeId", employeeId,
                "workDate", LocalDate.now().toString(),
                "status", "PRESENT"
        ));

        mockMvc.perform(post("/api/v1/attendance/mark")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attendancePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.status").value("PRESENT"));

        String leavePayload = objectMapper.writeValueAsString(java.util.Map.of(
                "employeeId", employeeId,
                "startDate", LocalDate.now().plusDays(2).toString(),
                "endDate", LocalDate.now().plusDays(4).toString(),
                "reason", "Family event"
        ));

        String leaveResponse = mockMvc.perform(post("/api/v1/leaves")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leavePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long leaveId = objectMapper.readTree(leaveResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/leaves/{id}/approve", leaveId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/attendance/employee/{employeeId}", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(employeeId));

        mockMvc.perform(get("/api/v1/leaves")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String adminToken() throws Exception {
        String loginPayload = """
                {
                  "email": "hr-admin@example.com",
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

        JsonNode json = objectMapper.readTree(loginResponse);
        return json.get("accessToken").asText();
    }
}
