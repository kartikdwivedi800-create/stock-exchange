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
                .orderType(requestDto.getOrderType())
                .price(requestDto.getPrice())
                .quantity(requestDto.getQuantity())
                .clientOrderId(requestDto.getClientOrderId())
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
                .orderType(order.getOrderType())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .status(order.getStatus())
                .filledQuantity(order.getFilledQuantity())
                .avgFillPrice(order.getAvgFillPrice())
                .rejectionReason(order.getRejectionReason())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
