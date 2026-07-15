package com.smartoffice.hr.leave.repository;

import com.smartoffice.hr.leave.domain.LeaveRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {
}
