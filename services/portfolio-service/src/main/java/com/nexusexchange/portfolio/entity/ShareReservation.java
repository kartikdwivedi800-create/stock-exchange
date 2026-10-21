package com.nexusexchange.portfolio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Mirrors table {@code share_reservations} (migration V2). One row per SELL order locking shares. */
@Entity
@Table(name = "share_reservations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The SELL order that triggered this reservation; unique so one order has one reservation. */
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holding_id", nullable = false)
    private Holding holding;

    /** Total shares locked at reservation time. */
    @Column(name = "quantity_reserved", nullable = false)
    private Long quantityReserved;

    /** Portion already consumed by fills; starts at 0, grows up to quantityReserved. */
    @Column(name = "quantity_used", nullable = false)
    private Long quantityUsed;

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
        if (quantityUsed == null) {
            quantityUsed = 0L;
        }
        if (status == null) {
            status = ReservationStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public String toString() {
        return "ShareReservation(id=" + id + ", orderId=" + orderId + ", quantityReserved=" + quantityReserved
                + ", quantityUsed=" + quantityUsed + ", status=" + status + ")";
    }
}