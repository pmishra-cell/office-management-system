package com.smartoffice.hr.leave.service;

import com.smartoffice.hr.employee.repository.EmployeeRepository;
import com.smartoffice.hr.leave.api.CreateLeaveRequest;
import com.smartoffice.hr.leave.api.LeaveResponse;
import com.smartoffice.hr.leave.domain.LeaveRequestEntity;
import com.smartoffice.hr.leave.domain.LeaveStatus;
import com.smartoffice.hr.leave.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public LeaveResponse create(CreateLeaveRequest request) {
        if (!employeeRepository.existsById(request.employeeId())) {
            throw new IllegalArgumentException("Employee not found");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }

        LeaveRequestEntity entity = new LeaveRequestEntity();
        entity.setEmployeeId(request.employeeId());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setReason(request.reason());
        entity.setStatus(LeaveStatus.REQUESTED);

        LeaveRequestEntity saved = leaveRequestRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> list() {
        return leaveRequestRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public LeaveResponse approve(Long id) {
        LeaveRequestEntity entity = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        entity.setStatus(LeaveStatus.APPROVED);
        return toResponse(leaveRequestRepository.save(entity));
    }

    @Transactional
    public LeaveResponse reject(Long id) {
        LeaveRequestEntity entity = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        entity.setStatus(LeaveStatus.REJECTED);
        return toResponse(leaveRequestRepository.save(entity));
    }

    private LeaveResponse toResponse(LeaveRequestEntity entity) {
        return new LeaveResponse(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
