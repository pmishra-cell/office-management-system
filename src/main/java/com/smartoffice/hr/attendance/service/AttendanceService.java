package com.smartoffice.hr.attendance.service;

import com.smartoffice.hr.attendance.api.AttendanceResponse;
import com.smartoffice.hr.attendance.api.MarkAttendanceRequest;
import com.smartoffice.hr.attendance.domain.AttendanceEntry;
import com.smartoffice.hr.attendance.repository.AttendanceEntryRepository;
import com.smartoffice.hr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceEntryRepository attendanceEntryRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public AttendanceResponse mark(MarkAttendanceRequest request) {
        if (!employeeRepository.existsById(request.employeeId())) {
            throw new IllegalArgumentException("Employee not found");
        }

        AttendanceEntry entry = new AttendanceEntry();
        entry.setEmployeeId(request.employeeId());
        entry.setWorkDate(request.workDate());
        entry.setStatus(request.status());
        entry.setCheckInAt(request.checkInAt() != null ? request.checkInAt() : Instant.now());

        AttendanceEntry saved = attendanceEntryRepository.save(entry);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> listByEmployee(Long employeeId) {
        return attendanceEntryRepository.findByEmployeeIdOrderByWorkDateDesc(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AttendanceResponse toResponse(AttendanceEntry entry) {
        return new AttendanceResponse(
                entry.getId(),
                entry.getEmployeeId(),
                entry.getWorkDate(),
                entry.getStatus(),
                entry.getCheckInAt()
        );
    }
}
