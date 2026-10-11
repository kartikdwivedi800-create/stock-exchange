package com.nexusexchange.stock.dto.response;

import com.nexusexchange.stock.entity.TradingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResponseDto {
    private Long id;
    private String symbol;
    private String companyName;
    private String exchange;
    private String sector;
    private BigDecimal tickSize;
    private Integer lotSize;
    private BigDecimal lastPrice;
    private BigDecimal previousClose;
    private BigDecimal dayOpen;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private Long dayVolume;
    private TradingStatus tradingStatus;
}
