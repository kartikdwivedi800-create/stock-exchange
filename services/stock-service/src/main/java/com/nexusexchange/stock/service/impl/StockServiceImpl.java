package com.nexusexchange.stock.service.impl;

import com.nexusexchange.stock.dto.request.StockRequestDto;
import com.nexusexchange.stock.dto.response.StockResponseDto;
import com.nexusexchange.stock.service.StockService;
import org.springframework.stereotype.Service;

@Service
public class StockServiceImpl implements StockService {

    @Override
    public StockResponseDto createStock(StockRequestDto requestDto) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public StockResponseDto getStock(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
