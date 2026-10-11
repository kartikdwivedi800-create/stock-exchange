package com.nexusexchange.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Mirrors table {@code stocks} (migration V2). One row per listed symbol. */
@Entity
@Table(name = "stocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false, length = 10)
    private String exchange;

    @Column(length = 50)
    private String sector;

    /** Minimum price increment for orders. */
    @Column(name = "tick_size", nullable = false, precision = 10, scale = 4)
    private BigDecimal tickSize;

    /** Minimum (and multiple of) order quantity. */
    @Column(name = "lot_size", nullable = false)
    private Integer lotSize;

    @Column(name = "last_price", precision = 19, scale = 4)
    private BigDecimal lastPrice;

    @Column(name = "previous_close", precision = 19, scale = 4)
    private BigDecimal previousClose;

    @Column(name = "day_open", precision = 19, scale = 4)
    private BigDecimal dayOpen;

    @Column(name = "day_high", precision = 19, scale = 4)
    private BigDecimal dayHigh;

    @Column(name = "day_low", precision = 19, scale = 4)
    private BigDecimal dayLow;

    @Column(name = "day_volume", nullable = false)
    private Long dayVolume;

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_status", nullable = false, length = 20)
    private TradingStatus tradingStatus;

    /** Optimistic locking: prevents lost updates when concurrent trades move the price. */
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
        if (exchange == null) {
            exchange = "NEX";
        }
        if (tickSize == null) {
            tickSize = new BigDecimal("0.05");
        }
        if (lotSize == null) {
            lotSize = 1;
        }
        if (dayVolume == null) {
            dayVolume = 0L;
        }
        if (tradingStatus == null) {
            tradingStatus = TradingStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public String toString() {
        return "Stock(id=" + id + ", symbol=" + symbol + ", lastPrice=" + lastPrice
                + ", tradingStatus=" + tradingStatus + ")";
    }
}
