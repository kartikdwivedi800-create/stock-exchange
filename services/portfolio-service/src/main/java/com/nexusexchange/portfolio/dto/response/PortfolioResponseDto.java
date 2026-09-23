package com.nexusexchange.portfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponseDto {
    private Long id;
    private Long userId;
    private String symbol;
    private Long quantity;
    private BigDecimal averageBuyPrice;
}
