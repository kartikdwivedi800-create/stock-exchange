package com.nexusexchange.stock.controller;

import com.nexusexchange.stock.dto.request.StockRequestDto;
import com.nexusexchange.stock.dto.response.StockResponseDto;
import com.nexusexchange.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping
    public ResponseEntity<StockResponseDto> createStock(@RequestBody StockRequestDto requestDto) {
        return ResponseEntity.ok(stockService.createStock(requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockResponseDto> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.getStock(id));
    }
}
