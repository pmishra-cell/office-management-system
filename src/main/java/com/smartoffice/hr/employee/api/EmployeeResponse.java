package com.smartoffice.hr.employee.api;

import java.time.Instant;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        String department,
        String title,
        Instant createdAt
) {
}
