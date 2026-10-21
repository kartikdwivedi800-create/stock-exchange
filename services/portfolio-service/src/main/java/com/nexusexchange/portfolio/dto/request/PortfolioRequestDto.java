package com.nexusexchange.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioRequestDto {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @PositiveOrZero(message = "Quantity must not be negative")
    private Long quantity;

    @PositiveOrZero(message = "Reserved quantity must not be negative")
    private Long reservedQuantity;

    @PositiveOrZero(message = "Average buy price must not be negative")
    private BigDecimal averageBuyPrice;

    private BigDecimal realizedPnl;
}