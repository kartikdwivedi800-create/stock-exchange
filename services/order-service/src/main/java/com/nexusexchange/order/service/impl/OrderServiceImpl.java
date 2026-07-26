package com.nexusexchange.order.service.impl;

import com.nexusexchange.order.dto.request.OrderRequestDto;
import com.nexusexchange.order.dto.response.OrderResponseDto;
import com.nexusexchange.order.service.OrderService;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    @Override
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        return null;
    }

    @Override
    public OrderResponseDto getOrder(Long id) {
        return null;
    }
}
