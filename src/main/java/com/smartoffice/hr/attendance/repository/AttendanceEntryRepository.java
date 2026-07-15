package com.smartoffice.hr.attendance.repository;

import com.smartoffice.hr.attendance.domain.AttendanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceEntryRepository extends JpaRepository<AttendanceEntry, Long> {
    List<AttendanceEntry> findByEmployeeIdOrderByWorkDateDesc(Long employeeId);
}
