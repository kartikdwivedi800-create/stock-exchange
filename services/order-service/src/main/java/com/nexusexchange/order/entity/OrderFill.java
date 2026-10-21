package com.nexusexchange.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors table {@code order_fills} (migration V2). Append-only; one row per trade applied to an order. */
@Entity
@Table(name = "order_fills")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Override
    public String toString() {
        return "OrderFill(id=" + id + ", tradeId=" + tradeId + ", price=" + price
                + ", quantity=" + quantity + ", executedAt=" + executedAt + ")";
    }
}
