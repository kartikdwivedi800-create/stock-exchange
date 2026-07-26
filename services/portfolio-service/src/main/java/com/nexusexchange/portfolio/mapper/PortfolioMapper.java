package com.nexusexchange.portfolio.mapper;

import com.nexusexchange.portfolio.dto.request.PortfolioRequestDto;
import com.nexusexchange.portfolio.dto.response.PortfolioResponseDto;
import com.nexusexchange.portfolio.entity.Portfolio;
import org.springframework.stereotype.Component;

@Component
public class PortfolioMapper {

    public Portfolio toEntity(PortfolioRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        return Portfolio.builder()
                .userId(requestDto.getUserId())
                .symbol(requestDto.getSymbol())
                .quantity(requestDto.getQuantity())
                .averageBuyPrice(requestDto.getAverageBuyPrice())
                .build();
    }

    public PortfolioResponseDto toResponseDto(Portfolio portfolio) {
        if (portfolio == null) {
            return null;
        }
        return PortfolioResponseDto.builder()
                .id(portfolio.getId())
                .userId(portfolio.getUserId())
                .symbol(portfolio.getSymbol())
                .quantity(portfolio.getQuantity())
                .averageBuyPrice(portfolio.getAverageBuyPrice())
                .build();
    }
}
