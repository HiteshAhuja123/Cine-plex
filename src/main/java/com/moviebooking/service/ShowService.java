package com.moviebooking.service;

import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.dto.response.ShowSeatResponse;
import com.moviebooking.entity.enums.ShowSeatStatus;
import com.moviebooking.entity.enums.ShowStatus;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.mapper.ShowMapper;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.ShowSeatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Show browsing and seat availability queries. */
@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    public ShowService(ShowRepository showRepository,
                       ShowSeatRepository showSeatRepository) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
    }

    /** Paginated list of upcoming shows for a given movie. */
    @Transactional(readOnly = true)
    public Page<ShowResponse> getShowsForMovie(Long movieId, Pageable pageable) {
        return showRepository
                .findByMovieIdAndStatusAndStartTimeAfter(
                        movieId, ShowStatus.ACTIVE, LocalDateTime.now(), pageable)
                .map(show -> {
                    long available = showSeatRepository.countByShowIdAndStatus(
                            show.getId(), ShowSeatStatus.AVAILABLE);
                    long total = show.getScreen().getTotalSeats();
                    return ShowMapper.toResponse(show, available, total);
                });
    }

    /**
     * All ShowSeats for a show — used by the seat-availability endpoint.
     * Groups are visible from the status field on each seat.
     */
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getSeatAvailability(Long showId) {
        verifyShowExists(showId);
        return showSeatRepository.findByShowId(showId).stream()
                .map(ShowMapper::toShowSeatResponse)
                .collect(Collectors.toList());
    }

    /** Only AVAILABLE seats — used by the AI assistant tool. */
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getAvailableSeats(Long showId) {
        verifyShowExists(showId);
        return showSeatRepository.findByShowIdAndStatus(showId, ShowSeatStatus.AVAILABLE)
                .stream()
                .map(ShowMapper::toShowSeatResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long showId) {
        return showRepository.findById(showId)
                .map(show -> {
                    long available = showSeatRepository.countByShowIdAndStatus(
                            show.getId(), ShowSeatStatus.AVAILABLE);
                    long total = show.getScreen().getTotalSeats();
                    return ShowMapper.toResponse(show, available, total);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
    }

    /** Internal helper for existence checks. */
    com.moviebooking.entity.Show getEntityById(Long showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
    }

    private void verifyShowExists(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show", showId);
        }
    }
}
