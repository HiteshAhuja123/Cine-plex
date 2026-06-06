package com.moviebooking.repository;

import com.moviebooking.entity.ShowSeat;
import com.moviebooking.entity.enums.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    // ── Standard availability query ───────────────────────────────────────────

    List<ShowSeat> findByShowIdAndStatus(Long showId, ShowSeatStatus status);

    List<ShowSeat> findByShowId(Long showId);

    long countByShowIdAndStatus(Long showId, ShowSeatStatus status);

    // ── THE CRITICAL LOCKING QUERY ────────────────────────────────────────────
    //
    // @Lock(PESSIMISTIC_WRITE) translates to SELECT ... FOR UPDATE in PostgreSQL.
    // When thread A holds this lock, thread B's identical query blocks until A's
    // transaction commits or rolls back. After A commits, B's rows have status=HELD
    // (or BOOKED), so B's availability check throws SeatNotAvailableException.
    //
    // Ordering by id ensures all threads acquire locks in the same order, which
    // prevents deadlocks when two transactions try to lock overlapping seat sets.
    /**
     * Acquires exclusive row locks on the given ShowSeat ids.
     * Called inside BookingService.holdSeats() before any status mutation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.id IN :ids ORDER BY ss.id ASC")
    List<ShowSeat> findByIdsWithPessimisticLock(@Param("ids") List<Long> ids);

    // ── Expired-holds detection (used by scheduler) ───────────────────────────

    /**
     * Returns ShowSeats still in HELD status whose hold timestamp has aged past
     * the hold window. The scheduler calls this every minute.
     */
    @Query("""
            SELECT ss FROM ShowSeat ss
            WHERE ss.status = 'HELD'
              AND ss.heldAt < :expiryBefore
            """)
    List<ShowSeat> findExpiredHolds(@Param("expiryBefore") LocalDateTime expiryBefore);
}
