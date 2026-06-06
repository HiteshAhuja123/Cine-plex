package com.moviebooking.service;

import com.moviebooking.dto.request.ConfirmBookingRequest;
import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.entity.*;
import com.moviebooking.entity.enums.BookingStatus;
import com.moviebooking.entity.enums.ShowSeatStatus;
import com.moviebooking.exception.*;
import com.moviebooking.mapper.BookingMapper;
import com.moviebooking.pricing.PricingStrategyFactory;
import com.moviebooking.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core booking orchestrator — the most concurrency-sensitive class in the system.
 *
 * <h2>Concurrency strategy</h2>
 * <p>The seat-hold flow uses <b>DB-level pessimistic locking</b>
 * ({@code SELECT ... FOR UPDATE} via JPA {@code PESSIMISTIC_WRITE}).
 *
 * <p>Flow for two concurrent threads T1 and T2 trying to book the same seat:
 * <ol>
 *   <li>Both call {@code holdSeats()}, both enter the transaction.</li>
 *   <li>T1 reaches {@link ShowSeatRepository#findByIdsWithPessimisticLock} first
 *       and acquires exclusive row locks.</li>
 *   <li>T2 issues the same {@code SELECT FOR UPDATE} — the DB blocks T2 until T1
 *       commits or rolls back.</li>
 *   <li>T1 validates seats as AVAILABLE, marks them HELD, commits → releases lock.</li>
 *   <li>T2 unblocks, re-reads the rows (now HELD), throws
 *       {@link SeatNotAvailableException}.</li>
 * </ol>
 *
 * <p>Ordering by id inside the lock query prevents deadlocks when overlapping seat
 * sets are held concurrently.
 *
 * <h2>Hold TTL</h2>
 * <p>Held seats expire after 5 minutes via {@link com.moviebooking.scheduler.SeatHoldExpiryScheduler}.
 */
@Service
public class BookingService {

    /** Hold window before auto-expiry. */
    private static final int HOLD_TTL_MINUTES = 5;

    private final BookingRepository bookingRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PricingStrategyFactory pricingStrategyFactory;

    public BookingService(BookingRepository bookingRepository,
                          ShowSeatRepository showSeatRepository,
                          ShowRepository showRepository,
                          UserRepository userRepository,
                          BookingSeatRepository bookingSeatRepository,
                          PricingStrategyFactory pricingStrategyFactory) {
        this.bookingRepository = bookingRepository;
        this.showSeatRepository = showSeatRepository;
        this.showRepository = showRepository;
        this.userRepository = userRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.pricingStrategyFactory = pricingStrategyFactory;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HOLD SEATS — the critical path
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Holds a set of seats for a user, creating a Booking in HELD status.
     *
     * <p>Uses {@link Isolation#READ_COMMITTED} (the PostgreSQL default) combined
     * with {@code PESSIMISTIC_WRITE} row-level locks acquired inside this
     * transaction. The lock is released when the transaction commits.
     *
     * @param request contains userId, showId, and the list of showSeatIds to hold
     * @return the created booking
     * @throws SeatNotAvailableException if any requested seat is not AVAILABLE
     * @throws ResourceNotFoundException if the user, show, or any seat is not found
     */
    @Transactional
    public BookingResponse holdSeats(HoldSeatsRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", request.showId()));

        // ── ACQUIRE PESSIMISTIC WRITE LOCKS ───────────────────────────────────
        // All seat IDs sorted ascending by the query (ORDER BY ss.id ASC) to avoid
        // deadlocks when two concurrent transactions lock overlapping seat sets.
        List<ShowSeat> lockedSeats = showSeatRepository
                .findByIdsWithPessimisticLock(request.showSeatIds());

        if (lockedSeats.size() != request.showSeatIds().size()) {
            throw new ResourceNotFoundException("One or more ShowSeats not found");
        }

        // ── VALIDATE ALL SEATS ARE AVAILABLE ─────────────────────────────────
        // This check is safe because we hold exclusive locks — no other transaction
        // can change these rows until we commit.
        List<String> unavailableSeats = lockedSeats.stream()
                .filter(ss -> ss.getStatus() != ShowSeatStatus.AVAILABLE)
                .map(ss -> ss.getSeat().getSeatCode())
                .collect(Collectors.toList());

        if (!unavailableSeats.isEmpty()) {
            throw new SeatNotAvailableException(
                    "Seats not available: " + String.join(", ", unavailableSeats), true);
        }

        // ── CALCULATE PRICES USING STRATEGY PATTERN ──────────────────────────
        // PricingStrategyFactory selects the right implementation at runtime
        // (weekday/weekend × regular/premium) — this is the polymorphism callsite.
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BigDecimal> prices = new ArrayList<>();
        for (ShowSeat ss : lockedSeats) {
            BigDecimal price = pricingStrategyFactory
                    .getStrategy(show, ss.getSeat())
                    .calculatePrice(show.getBasePrice(), show, ss.getSeat());
            prices.add(price);
            totalAmount = totalAmount.add(price);
        }

        // ── CREATE BOOKING ────────────────────────────────────────────────────
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking(user, show, totalAmount, now.plusMinutes(HOLD_TTL_MINUTES));
        Booking savedBooking = bookingRepository.save(booking);

        // ── MARK SEATS AS HELD AND LINK TO BOOKING ───────────────────────────
        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (int i = 0; i < lockedSeats.size(); i++) {
            ShowSeat ss = lockedSeats.get(i);
            ss.setStatus(ShowSeatStatus.HELD);
            ss.setHeldAt(now);
            ss.setHeldByBookingId(savedBooking.getId());
            ss.setPrice(prices.get(i));

            bookingSeats.add(new BookingSeat(savedBooking, ss));
        }

        bookingSeatRepository.saveAll(bookingSeats);
        savedBooking.setBookingSeats(bookingSeats);

        return BookingMapper.toResponse(savedBooking);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM BOOKING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Confirms a HELD booking, marking it and all its seats as BOOKED/CONFIRMED.
     *
     * @throws BookingExpiredException   if the 5-minute hold window has elapsed
     * @throws InvalidRequestException   if the booking does not belong to the user
     */
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, ConfirmBookingRequest request) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        return switch (booking.getStatus()) {
            case CONFIRMED -> BookingMapper.toResponse(booking); // idempotent
            case HELD -> {
                if (LocalDateTime.now().isAfter(booking.getExpiresAt())) {
                    throw new BookingExpiredException(bookingId);
                }
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setConfirmedAt(LocalDateTime.now());

                booking.getBookingSeats().forEach(bs -> {
                    ShowSeat ss = bs.getShowSeat();
                    ss.setStatus(ShowSeatStatus.BOOKED);
                    ss.setHeldAt(null);
                    ss.setHeldByBookingId(null);
                });

                yield BookingMapper.toResponse(bookingRepository.save(booking));
            }
            case EXPIRED -> throw new BookingExpiredException(bookingId);
            case CANCELLED -> throw new InvalidRequestException("Booking " + bookingId + " is cancelled");
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL BOOKING
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingMapper.toResponse(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);

        booking.getBookingSeats().forEach(bs -> {
            ShowSeat ss = bs.getShowSeat();
            ss.setStatus(ShowSeatStatus.AVAILABLE);
            ss.setHeldAt(null);
            ss.setHeldByBookingId(null);
        });

        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(BookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, Long userId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .map(BookingMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }
}
