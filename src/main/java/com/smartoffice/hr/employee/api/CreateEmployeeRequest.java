package com.smartoffice.hr.employee.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateEmployeeRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String department,
        @NotBlank String title
) {
}
