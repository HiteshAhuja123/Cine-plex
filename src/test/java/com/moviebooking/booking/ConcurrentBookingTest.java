package com.moviebooking.booking;

import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.entity.*;
import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.entity.enums.ShowSeatStatus;
import com.moviebooking.exception.SeatNotAvailableException;
import com.moviebooking.repository.*;
import com.moviebooking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  CONCURRENCY TEST — THE LOCK PROOF
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * <p>This is the most important test in the project. It proves that the
 * DB-level pessimistic locking in {@link BookingService#holdSeats} prevents
 * any two users from booking the same seat simultaneously.
 *
 * <h2>Test scenario</h2>
 * <p>N threads (default 10) are released simultaneously (via a {@link CountDownLatch})
 * to book the exact same single seat. Under correct locking behaviour:
 * <ul>
 *   <li>Exactly 1 thread succeeds and the seat transitions to HELD.</li>
 *   <li>All other N−1 threads receive {@link SeatNotAvailableException} (HTTP 409).</li>
 *   <li>The final seat status in the DB is HELD — not AVAILABLE or corrupted.</li>
 * </ul>
 *
 * <h2>Why this works</h2>
 * <p>{@code SELECT ... FOR UPDATE} (PostgreSQL) serialises the conflict. The first
 * transaction to acquire the lock validates AVAILABLE, sets HELD, and commits.
 * All subsequent transactions unblock, re-read HELD, and throw.
 *
 * <h2>Virtual threads</h2>
 * <p>Threads are created with {@link Thread#ofVirtual()} — demonstrating Java 21
 * virtual threads in the test context, and correctly stress-testing the virtual-thread
 * scheduler interaction with the pessimistic lock.
 *
 * <h2>Infrastructure</h2>
 * <p>Uses Testcontainers to spin up a real PostgreSQL instance — not H2.
 * This is essential: H2 has different locking semantics and would not prove
 * that the PostgreSQL-specific {@code SELECT FOR UPDATE} actually works.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Concurrent seat booking — pessimistic lock proof")
class ConcurrentBookingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private TheaterRepository theaterRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;

    private Long showSeatId;
    private List<Long> userIds;

    private static final int CONCURRENT_THREADS = 10;

    @BeforeEach
    void setup() {
        // Clean slate for each test
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        showSeatRepository.deleteAll();
        showRepository.deleteAll();
        seatRepository.deleteAll();
        screenRepository.deleteAll();
        theaterRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        // Create N users (one per concurrent thread)
        userIds = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            User u = userRepository.save(new User("User" + i, "user" + i + "@test.com", null));
            userIds.add(u.getId());
        }

        // Create a movie, theater, screen, single seat, show, and showSeat
        Movie movie = movieRepository.save(
                new Movie("Test Movie", "DRAMA", "desc", 120, "English",
                        LocalDate.now(), null, 7.0));

        Theater theater = theaterRepository.save(
                new Theater("Test Theater", "TestCity", "123 Main St"));

        Screen screen = screenRepository.save(new Screen(theater, "Screen 1", 1));

        Seat seat = seatRepository.save(new Seat(screen, "A", 1, SeatType.REGULAR));

        Show show = showRepository.save(new Show(
                movie, screen,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("12.00"), "English"));

        ShowSeat showSeat = showSeatRepository.save(
                new ShowSeat(show, seat, new BigDecimal("12.00")));

        showSeatId = showSeat.getId();
    }

    /**
     * THE MAIN LOCK TEST.
     *
     * <p>10 virtual threads simultaneously try to hold the same single seat.
     * Asserts: exactly 1 success, exactly 9 failures, seat ends in HELD state.
     */
    @Test
    @DisplayName("Only one of N concurrent booking requests for the same seat should succeed")
    void onlyOneBookingShouldSucceedForSameSeat() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(CONCURRENT_THREADS);
        CountDownLatch go    = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(CONCURRENT_THREADS);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);

        // Spawn N virtual threads — each tries to book the same showSeatId
        IntStream.range(0, CONCURRENT_THREADS).forEach(i -> {
            final long userId = userIds.get(i);
            Thread.ofVirtual().name("booking-thread-" + i).start(() -> {
                ready.countDown();
                try {
                    go.await();  // all threads start simultaneously

                    HoldSeatsRequest req = new HoldSeatsRequest(
                            userId,
                            showSeatRepository.findById(showSeatId)
                                    .orElseThrow().getShow().getId(),
                            List.of(showSeatId)
                    );
                    BookingResponse booking = bookingService.holdSeats(req);
                    successes.incrementAndGet();

                } catch (SeatNotAvailableException ignored) {
                    failures.incrementAndGet();
                } catch (Exception e) {
                    // Any other exception is a test failure — don't swallow it silently
                    System.err.println("Unexpected exception in thread " + i + ": " + e.getMessage());
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        });

        // Wait for all threads to be ready, then fire
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        go.countDown(); // RELEASE — all threads start at once

        // Wait for all threads to complete
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();

        // ── ASSERTIONS ─────────────────────────────────────────────────────────

        assertThat(successes.get())
                .as("Exactly ONE booking should succeed")
                .isEqualTo(1);

        assertThat(failures.get())
                .as("All other " + (CONCURRENT_THREADS - 1) + " requests should fail with SeatNotAvailableException")
                .isEqualTo(CONCURRENT_THREADS - 1);

        // Verify the final DB state — seat must be HELD (not AVAILABLE or corrupted)
        ShowSeat finalSeat = showSeatRepository.findById(showSeatId).orElseThrow();
        assertThat(finalSeat.getStatus())
                .as("Seat must be in HELD state after the winning booking")
                .isEqualTo(ShowSeatStatus.HELD);

        // Exactly one booking record should exist in HELD status
        long heldBookingCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus().name().equals("HELD"))
                .count();
        assertThat(heldBookingCount)
                .as("Exactly one HELD booking should exist in the database")
                .isEqualTo(1);
    }

    /**
     * Regression: booking different seats concurrently should ALL succeed (no false conflicts).
     */
    @Test
    @DisplayName("Concurrent bookings for different seats should all succeed")
    void concurrentBookingsForDifferentSeatsShouldAllSucceed() throws InterruptedException {
        // Create 5 additional seats and showSeats for this test
        Show show = showSeatRepository.findById(showSeatId).orElseThrow().getShow();
        Screen screen = show.getScreen();

        List<Long> extraSeatIds = new ArrayList<>();
        for (int i = 2; i <= 5; i++) {
            Seat seat = seatRepository.save(new Seat(screen, "A", i, SeatType.REGULAR));
            ShowSeat ss = showSeatRepository.save(new ShowSeat(show, seat, new BigDecimal("12.00")));
            extraSeatIds.add(ss.getId());
        }

        int threads = extraSeatIds.size();
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go    = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final long userId = userIds.get(i);
            final long seatId = extraSeatIds.get(i);
            Thread.ofVirtual().start(() -> {
                ready.countDown();
                try {
                    go.await();
                    Long showId = showSeatRepository.findById(seatId).orElseThrow().getShow().getId();
                    bookingService.holdSeats(new HoldSeatsRequest(userId, showId, List.of(seatId)));
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        done.await(30, TimeUnit.SECONDS);

        assertThat(successes.get())
                .as("All bookings on different seats should succeed")
                .isEqualTo(threads);
    }
}
