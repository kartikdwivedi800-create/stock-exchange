package com.nexusexchange.portfolio.mapper;

import com.nexusexchange.portfolio.dto.request.PortfolioRequestDto;
import com.nexusexchange.portfolio.dto.response.PortfolioResponseDto;
import com.nexusexchange.portfolio.entity.Holding;
import org.springframework.stereotype.Component;

@Component
public class PortfolioMapper {

    public Holding toEntity(PortfolioRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        return Holding.builder()
                .userId(requestDto.getUserId())
                .symbol(requestDto.getSymbol())
                .quantity(requestDto.getQuantity())
                .reservedQuantity(requestDto.getReservedQuantity())
                .averageBuyPrice(requestDto.getAverageBuyPrice())
                .realizedPnl(requestDto.getRealizedPnl())
                .build();
    }

    public PortfolioResponseDto toResponseDto(Holding holding) {
        if (holding == null) {
            return null;
        }
        return PortfolioResponseDto.builder()
                .id(holding.getId())
                .userId(holding.getUserId())
                .symbol(holding.getSymbol())
                .quantity(holding.getQuantity())
                .reservedQuantity(holding.getReservedQuantity())
                .averageBuyPrice(holding.getAverageBuyPrice())
                .realizedPnl(holding.getRealizedPnl())
                .build();
    }
}