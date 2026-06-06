package com.moviebooking.service;

import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.dto.response.TopMovieResponse;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.mapper.MovieMapper;
import com.moviebooking.repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Movie browsing service.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Paginated listing ({@link Page}) for the browse endpoint</li>
 *   <li>Streams + lambdas for mapping native query rows</li>
 *   <li>Optional chaining for safe entity lookup</li>
 * </ul>
 */
@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Paginated, keyword-searchable movie listing.
     * Keyword matches against title and genre (JPQL LIKE).
     */
    @Transactional(readOnly = true)
    public Page<MovieResponse> searchMovies(String keyword, Pageable pageable) {
        // Pass null to the JPQL query only when keyword is non-blank — Postgres cannot
        // infer the type of a null bind parameter inside lower(), causing a bytea cast error.
        if (keyword == null || keyword.isBlank()) {
            return movieRepository.findByActiveTrue(pageable).map(MovieMapper::toResponse);
        }
        return movieRepository.searchMovies(keyword, pageable).map(MovieMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        return movieRepository.findById(id)
                .filter(m -> m.getActive())
                .map(MovieMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    /**
     * Top N most-booked movies — uses the native SQL aggregation query.
     * Streams over Object[] rows and maps each via a method reference.
     */
    @Transactional(readOnly = true)
    public List<TopMovieResponse> getTopBookedMovies(int limit) {
        return movieRepository.findTopBookedMovies(limit).stream()
                .map(MovieMapper::toTopMovieResponse)
                .collect(Collectors.toList());
    }

    /** Used by AI tools and data initializer. */
    @Transactional(readOnly = true)
    public com.moviebooking.entity.Movie getEntityById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }
}
