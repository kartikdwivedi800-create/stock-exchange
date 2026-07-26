package com.nexusexchange.portfolio.service;

import com.nexusexchange.portfolio.dto.request.PortfolioRequestDto;
import com.nexusexchange.portfolio.dto.response.PortfolioResponseDto;

public interface PortfolioService {
    PortfolioResponseDto createPortfolio(PortfolioRequestDto requestDto);
    PortfolioResponseDto getPortfolio(Long id);
}
