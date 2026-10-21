package com.nexusexchange.stock.entity;

/** Values allowed by the CHECK constraint on {@code stocks.trading_status} (migration V2). */
public enum TradingStatus {
    ACTIVE,
    HALTED,
    DELISTED
}
