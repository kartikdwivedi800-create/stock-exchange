package com.nexusexchange.portfolio.entity;

/** Lifecycle of a {@link ShareReservation}: mirrors the CHECK constraint in migration V2. */
public enum ReservationStatus {
    /** Shares are locked; the associated SELL order is still open. */
    ACTIVE,
    /** Reservation was released because the order was cancelled; shares returned to available. */
    RELEASED,
    /** Reservation was fully consumed during trade settlement. */
    CONSUMED
}