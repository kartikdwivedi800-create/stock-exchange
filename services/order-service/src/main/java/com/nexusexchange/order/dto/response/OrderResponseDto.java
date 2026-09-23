package com.nexusexchange.order.dto.response;

import com.nexusexchange.order.entity.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private Long id;
    private String symbol;
    private OrderSide side;
    private BigDecimal price;
    private Long quantity;
}
