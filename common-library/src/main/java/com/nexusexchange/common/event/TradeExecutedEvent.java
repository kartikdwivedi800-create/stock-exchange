package com.nexusexchange.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TradeExecutedEvent extends BaseEvent {
    private Long tradeId;
    private Long buyOrderId;
    private Long sellOrderId;
    private String symbol;
    private BigDecimal price;
    private Long quantity;
    private LocalDateTime executionTime;
}
