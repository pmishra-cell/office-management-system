package com.smartoffice.hr.attendance.api;

import com.smartoffice.hr.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public AttendanceResponse mark(@Valid @RequestBody MarkAttendanceRequest request) {
        return attendanceService.mark(request);
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public List<AttendanceResponse> listByEmployee(@PathVariable("employeeId") Long employeeId) {
        return attendanceService.listByEmployee(employeeId);
    }
}
