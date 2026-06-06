package com.moviebooking.repository;

import com.moviebooking.entity.Show;
import com.moviebooking.entity.enums.ShowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    // ── Derived queries ───────────────────────────────────────────────────────

    Page<Show> findByMovieIdAndStatusAndStartTimeAfter(
            Long movieId, ShowStatus status, LocalDateTime after, Pageable pageable);

    List<Show> findByMovieIdAndStatus(Long movieId, ShowStatus status);

    // ── Custom JPQL — shows in a city after a given time ─────────────────────

    /**
     * Finds active shows for a given city, useful for geo-filtered discovery.
     * Demonstrates join traversal in JPQL (Show → Screen → Theater).
     */
    @Query("""
            SELECT s FROM Show s
            JOIN s.screen sc
            JOIN sc.theater t
            WHERE t.city = :city
              AND s.status = :status
              AND s.startTime > :from
            ORDER BY s.startTime ASC
            """)
    Page<Show> findByTheaterCityAndStatusAfter(
            @Param("city") String city,
            @Param("status") ShowStatus status,
            @Param("from") LocalDateTime from,
            Pageable pageable);
}
