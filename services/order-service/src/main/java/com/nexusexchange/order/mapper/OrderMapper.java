package com.nexusexchange.order.mapper;

import com.nexusexchange.order.dto.request.OrderRequestDto;
import com.nexusexchange.order.dto.response.OrderResponseDto;
import com.nexusexchange.order.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        return Order.builder()
                .symbol(requestDto.getSymbol())
                .side(requestDto.getSide())
                .price(requestDto.getPrice())
                .quantity(requestDto.getQuantity())
                .build();
    }

    public OrderResponseDto toResponseDto(Order order) {
        if (order == null) {
            return null;
        }
        return OrderResponseDto.builder()
                .id(order.getId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .build();
    }
}
