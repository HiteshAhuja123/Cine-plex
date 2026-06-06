package com.moviebooking.mapper;

import com.moviebooking.dto.response.UserResponse;
import com.moviebooking.entity.User;

/** Converts User entity to UserResponse DTO. Manual mapper — no MapStruct dependency needed. */
public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt()
        );
    }
}
