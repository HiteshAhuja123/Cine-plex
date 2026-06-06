package com.moviebooking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for POST /api/users. */
public record CreateUserRequest(

        @NotBlank(message = "name is required")
        @Size(min = 2, max = 100, message = "name must be 2–100 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone
) {}
