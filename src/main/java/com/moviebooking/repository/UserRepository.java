package com.moviebooking.repository;

import com.moviebooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Derived query — Spring Data generates the JPQL automatically. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
