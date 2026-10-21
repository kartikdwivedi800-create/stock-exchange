package com.nexusexchange.order.dto.response;

import com.nexusexchange.common.enums.OrderSide;
import com.nexusexchange.common.enums.OrderStatus;
import com.nexusexchange.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private Long id;
    private String symbol;
    private OrderSide side;
    private OrderType orderType;
    private BigDecimal price;
    private Long quantity;
    private OrderStatus status;
    private Long filledQuantity;
    private BigDecimal avgFillPrice;
    private String rejectionReason;
    private Instant createdAt;
}
