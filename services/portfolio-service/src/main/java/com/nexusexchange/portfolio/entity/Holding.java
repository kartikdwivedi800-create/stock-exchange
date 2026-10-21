package com.nexusexchange.portfolio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Mirrors table {@code holdings} (migration V2). One row per (user, symbol); replaces the V1 portfolios table. */
@Entity
@Table(name = "holdings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    /** Total shares owned, including those reserved by open SELL orders. */
    @Column(nullable = false)
    private Long quantity;

    /** Shares locked by open SELL orders; never exceeds quantity. */
    @Column(name = "reserved_quantity", nullable = false)
    private Long reservedQuantity;

    @Column(name = "average_buy_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageBuyPrice;

    /** Cumulative realized profit/loss; negative for losses. */
    @Column(name = "realized_pnl", nullable = false, precision = 19, scale = 4)
    private BigDecimal realizedPnl;

    /** Optimistic locking: prevents lost updates when concurrent trades settle. */
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
        if (quantity == null) {
            quantity = 0L;
        }
        if (reservedQuantity == null) {
            reservedQuantity = 0L;
        }
        if (averageBuyPrice == null) {
            averageBuyPrice = BigDecimal.ZERO;
        }
        if (realizedPnl == null) {
            realizedPnl = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public String toString() {
        return "Holding(id=" + id + ", userId=" + userId + ", symbol=" + symbol
                + ", quantity=" + quantity + ", reservedQuantity=" + reservedQuantity + ")";
    }
}