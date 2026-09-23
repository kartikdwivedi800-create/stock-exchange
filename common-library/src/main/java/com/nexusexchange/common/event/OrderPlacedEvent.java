package com.nexusexchange.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OrderPlacedEvent extends BaseEvent {
    private Long orderId;
    private Long userId;
    private String symbol;
    private String side;
    private BigDecimal price;
    private Long quantity;
    private String orderType;
}
