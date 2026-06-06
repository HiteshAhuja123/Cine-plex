package com.moviebooking.repository;

import com.moviebooking.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // ── Derived query methods ─────────────────────────────────────────────────

    /** Paginated search by title (case-insensitive partial match). */
    Page<Movie> findByTitleContainingIgnoreCaseAndActiveTrue(String title, Pageable pageable);

    /** Filter by genre with pagination — used by browse endpoint. */
    Page<Movie> findByGenreIgnoreCaseAndActiveTrue(String genre, Pageable pageable);

    Page<Movie> findByActiveTrue(Pageable pageable);

    boolean existsByTitle(String title);

    // ── Custom JPQL query ─────────────────────────────────────────────────────

    /**
     * Finds active movies matching title OR genre keyword, with pagination.
     * Demonstrates a custom @Query with named parameters.
     */
    @Query("""
            SELECT m FROM Movie m
            WHERE m.active = true
              AND (:keyword IS NULL
                   OR LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY m.averageRating DESC NULLS LAST
            """)
    Page<Movie> searchMovies(@Param("keyword") String keyword, Pageable pageable);

    // ── Native SQL query — aggregation (top-booked movies) ───────────────────

    /**
     * Returns the top N most-booked movies by confirmed booking count.
     * Native SQL to demonstrate raw query skill and use of LIMIT.
     * Each row: [movie_id, title, genre, booking_count].
     */
    @Query(value = """
            SELECT m.id, m.title, m.genre, m.poster_url,
                   COUNT(bs.id) AS booking_count
            FROM movies m
            JOIN shows s   ON s.movie_id  = m.id
            JOIN bookings b ON b.show_id  = s.id AND b.status = 'CONFIRMED'
            JOIN booking_seats bs ON bs.booking_id = b.id
            WHERE m.active = true
            GROUP BY m.id, m.title, m.genre, m.poster_url
            ORDER BY booking_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopBookedMovies(@Param("limit") int limit);
}
