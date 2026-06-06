package com.moviebooking.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDateTime createdAt
) {}
