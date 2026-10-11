package com.nexusexchange.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/** Mirrors table {@code candles} (migration V2). One OHLCV bar per symbol, timeframe and open time. */
@Entity
@Table(name = "candles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Candle {

    @EmbeddedId
    private CandleId id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal open;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal high;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal low;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;
}
