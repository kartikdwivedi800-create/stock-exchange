package com.nexusexchange.wallet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Mirrors table {@code fund_reservations} (migration V2). One row per BUY order locking funds. */
@Entity
@Table(name = "fund_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The BUY order that triggered this reservation; unique so one order → one reservation. */
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /** Total cash locked at reservation time. */
    @Column(name = "amount_reserved", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountReserved;

    /** Portion already consumed by fills; starts at 0, grows up to amountReserved. */
    @Column(name = "amount_used", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    /** Optimistic locking: concurrent consumption of the same reservation will be detected. */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        createdAt = now;
        updatedAt = now;
        if (amountUsed == null) {
            amountUsed = BigDecimal.ZERO;
        }
        if (status == null) {
            status = ReservationStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
