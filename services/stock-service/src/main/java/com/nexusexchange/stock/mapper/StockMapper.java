package com.nexusexchange.stock.mapper;

import com.nexusexchange.stock.dto.request.StockRequestDto;
import com.nexusexchange.stock.dto.response.StockResponseDto;
import com.nexusexchange.stock.entity.Stock;
import org.springframework.stereotype.Component;

@Component
public class StockMapper {

    public Stock toEntity(StockRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        return Stock.builder()
                .symbol(requestDto.getSymbol())
                .companyName(requestDto.getCompanyName())
                .price(requestDto.getPrice())
                .build();
    }

    public StockResponseDto toResponseDto(Stock stock) {
        if (stock == null) {
            return null;
        }
        return StockResponseDto.builder()
                .id(stock.getId())
                .symbol(stock.getSymbol())
                .companyName(stock.getCompanyName())
                .price(stock.getPrice())
                .build();
    }
}
