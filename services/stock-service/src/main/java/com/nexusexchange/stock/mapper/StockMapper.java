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
                .sector(requestDto.getSector())
                .tickSize(requestDto.getTickSize())
                .lotSize(requestDto.getLotSize())
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
                .exchange(stock.getExchange())
                .sector(stock.getSector())
                .tickSize(stock.getTickSize())
                .lotSize(stock.getLotSize())
                .lastPrice(stock.getLastPrice())
                .previousClose(stock.getPreviousClose())
                .dayOpen(stock.getDayOpen())
                .dayHigh(stock.getDayHigh())
                .dayLow(stock.getDayLow())
                .dayVolume(stock.getDayVolume())
                .tradingStatus(stock.getTradingStatus())
                .build();
    }
}
