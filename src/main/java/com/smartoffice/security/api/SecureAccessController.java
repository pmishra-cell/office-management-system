package com.smartoffice.security.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/secure")
public class SecureAccessController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "message", "Authenticated",
                "principal", authentication.getName(),
                "authorities", authentication.getAuthorities().stream().map(Object::toString).toList()
        );
    }

    @GetMapping("/employee")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public Map<String, String> employeeArea() {
        return Map.of("message", "Employee area access granted");
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String, String> managerArea() {
        return Map.of("message", "Manager area access granted");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminArea() {
        return Map.of("message", "Admin area access granted");
    }
}