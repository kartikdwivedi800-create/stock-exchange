package com.nexusexchange.stock.repository;

import com.nexusexchange.testsupport.PostgresRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.nexusexchange.stock.entity.Candle;
import com.nexusexchange.stock.entity.CandleId;
import com.nexusexchange.stock.entity.Stock;
import com.nexusexchange.stock.entity.StockPriceHistory;
import com.nexusexchange.stock.entity.TradingStatus;

import java.time.Instant;

/** stock_db: V2 schema + V3 seed, entity mapping (incl. composite key) and constraints. */
@PostgresRepositoryTest
class StockRepositoryTest {

    @Autowired StockRepository stocks;
    @Autowired StockPriceHistoryRepository history;
    @Autowired CandleRepository candles;

    @Test
    void seedLoadsTwentyStocks() {
        assertThat(stocks.count()).isEqualTo(20);
        Stock tcs = stocks.findBySymbol("TCS").orElseThrow();
        assertThat(tcs.getSector()).isEqualTo("IT");
        assertThat(tcs.getTradingStatus()).isEqualTo(TradingStatus.ACTIVE);
        assertThat(tcs.getTickSize()).isEqualByComparingTo("0.05");
    }

    @Test
    void symbolIsUnique() {
        Stock duplicate = Stock.builder().symbol("TCS").companyName("Duplicate").build();

        assertThatThrownBy(() -> stocks.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void priceHistoryMustReferenceAListedStock() {
        StockPriceHistory tick = StockPriceHistory.builder().symbol("NOPE")
                .price(new BigDecimal("10")).quantity(1L).build();

        assertThatThrownBy(() -> history.saveAndFlush(tick))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void candleRoundTripsByCompositeKey() {
        CandleId id = new CandleId("TCS", "1m", Instant.parse("2026-10-22T03:45:00Z"));
        candles.saveAndFlush(Candle.builder().id(id).open(new BigDecimal("3500")).high(new BigDecimal("3510"))
                .low(new BigDecimal("3495")).close(new BigDecimal("3505")).volume(120L).build());

        Candle found = candles.findById(new CandleId("TCS", "1m", Instant.parse("2026-10-22T03:45:00Z"))).orElseThrow();
        assertThat(found.getClose()).isEqualByComparingTo("3505");
    }
}
