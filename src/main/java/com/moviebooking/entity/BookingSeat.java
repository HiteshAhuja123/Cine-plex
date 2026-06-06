package com.moviebooking.entity;

import jakarta.persistence.*;

/** Join between Booking and the ShowSeat rows it covers. */
@Entity
@Table(name = "booking_seats",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_booking_seats_booking_show_seat",
           columnNames = {"booking_id", "show_seat_id"}))
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;

    // ── Constructors ─────────────────────────────────────────────────────────

    public BookingSeat() {}

    public BookingSeat(Booking booking, ShowSeat showSeat) {
        this.booking = booking;
        this.showSeat = showSeat;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public ShowSeat getShowSeat() { return showSeat; }
    public void setShowSeat(ShowSeat showSeat) { this.showSeat = showSeat; }
}
