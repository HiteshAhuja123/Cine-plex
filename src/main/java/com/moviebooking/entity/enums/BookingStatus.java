package com.moviebooking.entity.enums;

/**
 * Lifecycle of a Booking.
 *
 * <pre>
 * HELD ──(confirm)──► CONFIRMED
 *  │                        │
 *  └──(cancel/expire)──► CANCELLED / EXPIRED
 * </pre>
 *
 * HELD has a 5-minute TTL enforced by SeatHoldExpiryScheduler.
 */
public enum BookingStatus {
    HELD,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
