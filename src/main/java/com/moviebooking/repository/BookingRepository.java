package com.moviebooking.repository;

import com.moviebooking.entity.Booking;
import com.moviebooking.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ── Derived queries ───────────────────────────────────────────────────────

    /** User's booking history — paginated and sorted by most recent. */
    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    // ── Custom JPQL — expired HELD bookings ──────────────────────────────────

    /**
     * Returns bookings still in HELD state whose expiry has passed.
     * Called by the scheduler alongside ShowSeat hold expiry cleanup.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = com.moviebooking.entity.enums.BookingStatus.HELD
              AND b.expiresAt < :now
            """)
    List<Booking> findExpiredHeldBookings(@Param("now") LocalDateTime now);

    List<Booking> findByStatusAndShowId(BookingStatus status, Long showId);
}
