package com.smartoffice.auth.api;

public record UserResponse(
        Long id,
        String email,
        String name,
        String role
) {
}
