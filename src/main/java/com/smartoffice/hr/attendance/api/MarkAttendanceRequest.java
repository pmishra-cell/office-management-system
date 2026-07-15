package com.smartoffice.hr.attendance.api;

import com.smartoffice.hr.attendance.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public record MarkAttendanceRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate workDate,
        @NotNull AttendanceStatus status,
        Instant checkInAt
) {
}
