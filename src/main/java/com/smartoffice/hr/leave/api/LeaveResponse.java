package com.smartoffice.hr.leave.api;

import com.smartoffice.hr.leave.domain.LeaveStatus;

import java.time.Instant;
import java.time.LocalDate;

public record LeaveResponse(
        Long id,
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveStatus status,
        Instant createdAt
) {
}
