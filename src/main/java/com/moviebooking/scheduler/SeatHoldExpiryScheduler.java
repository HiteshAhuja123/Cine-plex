package com.moviebooking.scheduler;

import com.moviebooking.entity.Booking;
import com.moviebooking.entity.ShowSeat;
import com.moviebooking.entity.enums.BookingStatus;
import com.moviebooking.entity.enums.ShowSeatStatus;
import com.moviebooking.repository.BookingRepository;
import com.moviebooking.repository.ShowSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background job that releases expired seat holds.
 *
 * <h2>Virtual threads</h2>
 * <p>The scheduler task executor is configured in {@link com.moviebooking.config.SchedulerConfig}
 * to use {@link Thread#ofVirtual()} — demonstrating Java 21 virtual thread usage.
 * Virtual threads are cheap to create and block without tying up OS threads, making
 * them ideal for I/O-bound tasks like this JDBC-backed scheduler.
 *
 * <h2>Why a scheduler instead of DB TTL?</h2>
 * <p>A DB trigger could also expire holds, but keeping the expiry logic in application
 * code makes it observable (logs, metrics) and testable without a DB-specific feature.
 *
 * <p>Runs every 60 seconds. Finds all ShowSeats with status=HELD whose heldAt is
 * more than 5 minutes ago, resets them to AVAILABLE, and marks the owning Booking EXPIRED.
 */
@Component
public class SeatHoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldExpiryScheduler.class);
    private static final int HOLD_TTL_MINUTES = 5;

    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;

    public SeatHoldExpiryScheduler(ShowSeatRepository showSeatRepository,
                                   BookingRepository bookingRepository) {
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Scans for expired holds every 60 seconds (fixedDelay = time between the end
     * of one run and the start of the next, so no pile-up if the job takes > 60s).
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime expiryBefore = LocalDateTime.now().minusMinutes(HOLD_TTL_MINUTES);

        List<ShowSeat> expired = showSeatRepository.findExpiredHolds(expiryBefore);
        if (expired.isEmpty()) {
            return;
        }

        log.info("Expiry scheduler: releasing {} stale seat holds (held before {})",
                expired.size(), expiryBefore);

        // Reset each expired ShowSeat back to AVAILABLE
        expired.forEach(ss -> {
            ss.setStatus(ShowSeatStatus.AVAILABLE);
            ss.setHeldAt(null);
            ss.setHeldByBookingId(null);
        });

        // Also expire the corresponding Booking records
        List<Booking> expiredBookings = bookingRepository.findExpiredHeldBookings(LocalDateTime.now());
        expiredBookings.forEach(b -> b.setStatus(BookingStatus.EXPIRED));

        log.debug("Expired {} ShowSeats and {} Bookings", expired.size(), expiredBookings.size());
    }
}
