package com.moviebooking.service;

import com.moviebooking.dto.request.CreateUserRequest;
import com.moviebooking.dto.response.UserResponse;
import com.moviebooking.entity.User;
import com.moviebooking.exception.InvalidRequestException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.mapper.UserMapper;
import com.moviebooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management — create and retrieve users.
 * Auth/JWT is out of scope and noted as future work.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new InvalidRequestException("A user with email '" + request.email() + "' already exists");
        }
        User user = new User(request.name(), request.email(), request.phone());
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for email: " + email));
    }

    /** Package-private — used by BookingService to validate user existence without mapping. */
    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
