package com.smartoffice.hr.attendance.api;

import com.smartoffice.hr.attendance.domain.AttendanceStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceResponse(
        Long id,
        Long employeeId,
        LocalDate workDate,
        AttendanceStatus status,
        Instant checkInAt
) {
}
