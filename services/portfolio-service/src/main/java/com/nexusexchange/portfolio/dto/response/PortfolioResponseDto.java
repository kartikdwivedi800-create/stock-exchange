package com.nexusexchange.portfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponseDto {
    private Long id;
    private Long userId;
    private String symbol;
    private Double quantity;
    private Double averageBuyPrice;
}
