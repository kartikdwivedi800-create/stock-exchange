package com.nexusexchange.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Composite primary key of table {@code candles} (migration V2): (symbol, timeframe, open_time). */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CandleId implements Serializable {

    @Column(nullable = false, length = 20)
    private String symbol;

    /** One of 1m, 5m, 15m, 1h, 1d (enforced by a CHECK constraint). */
    @Column(nullable = false, length = 5)
    private String timeframe;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CandleId other)) {
            return false;
        }
        return Objects.equals(symbol, other.symbol)
                && Objects.equals(timeframe, other.timeframe)
                && Objects.equals(openTime, other.openTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, timeframe, openTime);
    }
}
