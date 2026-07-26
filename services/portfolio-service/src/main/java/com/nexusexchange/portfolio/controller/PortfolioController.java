package com.nexusexchange.portfolio.controller;

import com.nexusexchange.portfolio.dto.request.PortfolioRequestDto;
import com.nexusexchange.portfolio.dto.response.PortfolioResponseDto;
import com.nexusexchange.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping
    public ResponseEntity<PortfolioResponseDto> createPortfolio(@RequestBody PortfolioRequestDto requestDto) {
        return ResponseEntity.ok(portfolioService.createPortfolio(requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponseDto> getPortfolio(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.getPortfolio(id));
    }
}
