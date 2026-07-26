package com.nexusexchange.stock.service;

import com.nexusexchange.stock.dto.request.StockRequestDto;
import com.nexusexchange.stock.dto.response.StockResponseDto;

public interface StockService {
    StockResponseDto createStock(StockRequestDto requestDto);
    StockResponseDto getStock(Long id);
}
