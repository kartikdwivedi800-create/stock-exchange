package com.nexusexchange.wallet.entity;

/** Lifecycle of a {@link FundReservation}: mirrors the CHECK constraint in migration V2. */
public enum ReservationStatus {
    /** Funds are locked; the associated order is still open. */
    ACTIVE,
    /** Reservation was released because the order was cancelled; funds returned to available. */
    RELEASED,
    /** Reservation was fully consumed during trade settlement. */
    CONSUMED
}
