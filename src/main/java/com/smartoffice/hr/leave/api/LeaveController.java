package com.smartoffice.hr.leave.api;

import com.smartoffice.hr.leave.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public LeaveResponse create(@Valid @RequestBody CreateLeaveRequest request) {
        return leaveService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<LeaveResponse> list() {
        return leaveService.list();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public LeaveResponse approve(@PathVariable("id") Long id, @RequestBody(required = false) LeaveDecisionRequest request) {
        return leaveService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public LeaveResponse reject(@PathVariable("id") Long id, @RequestBody(required = false) LeaveDecisionRequest request) {
        return leaveService.reject(id);
    }
}
