package com.nexusexchange.order.service;

import com.nexusexchange.order.dto.request.OrderRequestDto;
import com.nexusexchange.order.dto.response.OrderResponseDto;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto requestDto);
    OrderResponseDto getOrder(Long id);
}
