package com.moviebooking.entity;

import com.moviebooking.entity.enums.ShowSeatStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Join entity — represents one Seat's availability state within a specific Show.
 *
 * <h3>Concurrency safety</h3>
 * <p>BookingService acquires a {@code PESSIMISTIC_WRITE} lock on these rows before
 * mutating {@code status}. This is the DB-level lock that prevents double-booking.
 *
 * <h3>Optimistic locking alternative</h3>
 * <p>Adding {@code @Version private int version;} enables optimistic locking:
 * Hibernate appends {@code WHERE version = ?} on UPDATE, throwing
 * {@code ObjectOptimisticLockingFailureException} on conflict. Trade-offs vs. pessimistic:
 * <ul>
 *   <li><b>Optimistic</b> — no DB lock held across the transaction; better for low-contention
 *       workloads but the caller must handle the retry loop.</li>
 *   <li><b>Pessimistic (current)</b> — serialises concurrent access at the DB row level;
 *       simpler failure semantics (one winner, clear losers) at the cost of potential
 *       lock-wait timeout under extreme contention.</li>
 * </ul>
 */
@Entity
@Table(name = "show_seats",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_show_seats_show_seat",
           columnNames = {"show_id", "seat_id"}),
       indexes = {
           @Index(name = "idx_show_seats_show_status", columnList = "show_id, status"),
           @Index(name = "idx_show_seats_held_at", columnList = "held_at")
       })
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowSeatStatus status = ShowSeatStatus.AVAILABLE;

    /** Timestamp when status changed to HELD. Used by scheduler to detect expired holds. */
    @Column(name = "held_at")
    private LocalDateTime heldAt;

    /** Booking that placed the hold — used by the expiry scheduler to EXPIRE the booking too. */
    @Column(name = "held_by_booking_id")
    private Long heldByBookingId;

    /** Calculated price (base × strategy multiplier) — stored at hold time so confirmation is stable. */
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // ── Constructors ─────────────────────────────────────────────────────────

    public ShowSeat() {}

    public ShowSeat(Show show, Seat seat, BigDecimal price) {
        this.show = show;
        this.seat = seat;
        this.price = price;
        this.status = ShowSeatStatus.AVAILABLE;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }
    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }
    public ShowSeatStatus getStatus() { return status; }
    public void setStatus(ShowSeatStatus status) { this.status = status; }
    public LocalDateTime getHeldAt() { return heldAt; }
    public void setHeldAt(LocalDateTime heldAt) { this.heldAt = heldAt; }
    public Long getHeldByBookingId() { return heldByBookingId; }
    public void setHeldByBookingId(Long heldByBookingId) { this.heldByBookingId = heldByBookingId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
